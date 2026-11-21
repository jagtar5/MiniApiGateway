package com.mag.core.upstream.impl;

import com.mag.core.cache.BoundedLruCache;
import com.mag.core.cache.CacheEntry;
import com.mag.core.cache.CacheKey;
import com.mag.core.clock.Clock;
import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M-C: Fast-Path Cache — Proxy pattern decorator around {@link UpstreamClient}.
 *
 * Responsibilities (SRP: caching only):
 *   - Cache only safe GET responses with status 200 (MAG-FR-03).
 *   - Enforce per-entry TTL using injected {@link Clock} (MAG-QR-10).
 *   - Bounded LRU eviction so memory stays within limits (MAG-QR-09).
 *   - Single-flight loading: concurrent cache misses for the same key produce one upstream call.
 *   - User isolation: prevents cross-user cache hits (MAG-QR-06).
 *   - Exclude private and Set-Cookie responses from caching.
 *   - A cache hit never reaches the circuit breaker (ResilientUpstreamClient is bypassed).
 */
public class CachingUpstreamClient implements UpstreamClient {

    public static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
    public static final int DEFAULT_MAX_CAPACITY = 1000;

    private static final Pattern MAX_AGE_PATTERN = Pattern.compile("max-age\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final UpstreamClient delegate;
    private final Clock clock;
    private final Duration defaultTtl;
    private final BoundedLruCache<CacheKey, CacheEntry> cache;
    private final ConcurrentHashMap<CacheKey, CompletableFuture<UpstreamResponse>> inFlight = new ConcurrentHashMap<>();

    public CachingUpstreamClient(UpstreamClient delegate, Clock clock) {
        this(delegate, clock, DEFAULT_TTL, DEFAULT_MAX_CAPACITY);
    }

    public CachingUpstreamClient(UpstreamClient delegate, Clock clock, Duration defaultTtl, int maxCapacity) {
        this(delegate, clock, new BoundedLruCache<>(maxCapacity), defaultTtl);
    }

    public CachingUpstreamClient(UpstreamClient delegate, Clock clock, BoundedLruCache<CacheKey, CacheEntry> cache, Duration defaultTtl) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.defaultTtl = Objects.requireNonNull(defaultTtl, "defaultTtl");
    }

    @Override
    public UpstreamResponse execute(GatewayRequest request) {
        Objects.requireNonNull(request, "request");

        // Step 1: Bypass cache for non-GET methods
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return delegate.execute(request);
        }

        CacheKey key = CacheKey.fromRequest(request);

        // Step 2: Check cache for valid (non-expired) entry
        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired(clock)) {
            return cached.getResponse();
        }

        // Step 3: Single-flight loading on cache miss
        return executeSingleFlight(key, request);
    }

    private UpstreamResponse executeSingleFlight(CacheKey key, GatewayRequest request) {
        CompletableFuture<UpstreamResponse> future = new CompletableFuture<>();
        CompletableFuture<UpstreamResponse> leader = inFlight.putIfAbsent(key, future);

        if (leader != null) {
            // Wait for leader thread to finish upstream fetch
            return joinLeader(leader);
        }

        // This thread won the race and coordinates upstream load
        try {
            CacheEntry doubleCheck = cache.get(key);
            if (doubleCheck != null && !doubleCheck.isExpired(clock)) {
                future.complete(doubleCheck.getResponse());
                return doubleCheck.getResponse();
            }

            UpstreamResponse response = delegate.execute(request);
            if (isCacheable(request, response)) {
                Instant expiresAt = calculateExpiry(response);
                cache.put(key, new CacheEntry(response, expiresAt));
            }
            future.complete(response);
            return response;
        } catch (Throwable t) {
            future.completeExceptionally(t);
            throw unwrapOrRethrow(t);
        } finally {
            inFlight.remove(key, future);
        }
    }

    private UpstreamResponse joinLeader(CompletableFuture<UpstreamResponse> leader) {
        try {
            return leader.join();
        } catch (CompletionException ce) {
            throw unwrapOrRethrow(ce.getCause());
        }
    }

    private RuntimeException unwrapOrRethrow(Throwable t) {
        if (t instanceof RuntimeException re) {
            return re;
        }
        return new RuntimeException(t);
    }

    public boolean isCacheable(GatewayRequest request, UpstreamResponse response) {
        if (response == null || !"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (response.getStatusCode() != 200) {
            return false;
        }
        if (hasHeader(response, "Set-Cookie")) {
            return false;
        }

        String cacheControl = getHeaderValue(response, "Cache-Control");
        if (cacheControl != null) {
            String lower = cacheControl.toLowerCase();
            return !lower.contains("private") && !lower.contains("no-store");
        }
        return true;
    }

    public Instant calculateExpiry(UpstreamResponse response) {
        String cacheControl = getHeaderValue(response, "Cache-Control");
        if (cacheControl != null) {
            Matcher matcher = MAX_AGE_PATTERN.matcher(cacheControl);
            if (matcher.find()) {
                long maxAgeSeconds = Long.parseLong(matcher.group(1));
                if (maxAgeSeconds >= 0) {
                    return clock.now().plusSeconds(maxAgeSeconds);
                }
            }
        }
        return clock.now().plus(defaultTtl);
    }

    private boolean hasHeader(UpstreamResponse response, String headerName) {
        for (String key : response.getHeaders().keySet()) {
            if (key.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    private String getHeaderValue(UpstreamResponse response, String headerName) {
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public BoundedLruCache<CacheKey, CacheEntry> getCache() {
        return cache;
    }

    public UpstreamClient getDelegate() {
        return delegate;
    }

    public Clock getClock() {
        return clock;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public int getInFlightCount() {
        return inFlight.size();
    }
}
