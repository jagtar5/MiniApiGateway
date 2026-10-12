package com.mag.core.upstream.impl;

import com.mag.core.clock.Clock;
import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * M-C: Fast-Path Cache — Proxy pattern decorator around {@link UpstreamClient}.
 *
 * Responsibilities (SRP: caching only):
 *   - Cache only GET responses with status 200 (MAG-FR-03).
 *   - Enforce per-entry TTL using injected {@link Clock} (no real time, MAG-QR-10).
 *   - Bounded LRU eviction so memory stays within limits (MAG-QR-09).
 *   - Single-flight: concurrent cache misses for the same key produce one upstream call.
 *   - Never serve one user's response to another (MAG-QR-06).
 *
 * A cache hit never reaches the circuit breaker (correct architecture per component diagram).
 */
public class CachingUpstreamClient implements UpstreamClient {

    private final UpstreamClient delegate;
    private final Clock clock;

    // TODO M2: inject BoundedLruCache and ConcurrentHashMap<CacheKey, Future<UpstreamResponse>>

    public CachingUpstreamClient(UpstreamClient delegate, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public UpstreamResponse execute(GatewayRequest request) {
        // TODO M2:
        //   1. If method != GET, bypass the cache entirely.
        //   2. Build a user-scoped cache key.
        //   3. Check the cache; return if valid (not expired).
        //   4. Single-flight: compute entry, let concurrent callers wait for the same Future.
        //   5. Store on 200; skip Set-Cookie and Cache-Control: private responses.
        throw new UnsupportedOperationException("CachingUpstreamClient.execute — implement in M2");
    }
}
