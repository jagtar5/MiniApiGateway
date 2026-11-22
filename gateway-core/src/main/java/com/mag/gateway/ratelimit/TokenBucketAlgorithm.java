package com.mag.gateway.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket: capacity = limit, refills continuously at limit / window.
 * Absorbs bursts up to the capacity but cannot be gamed at a window boundary.
 *
 * <p>Thread-safety: the whole read-refill-consume-write cycle for one key runs inside
 * {@link ConcurrentHashMap#compute}, which is atomic per key. Bucket state is an immutable record.
 */
public final class TokenBucketAlgorithm implements RateLimitAlgorithm {

    private record Bucket(double tokens, long lastRefillMillis) { }

    private final int capacity;
    private final double refillPerMilli;
    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketAlgorithm(int limit, Duration window, Clock clock) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        this.capacity = limit;
        this.refillPerMilli = (double) limit / Keys.windowMillis(window);
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @Override
    public boolean tryAcquire(String key) {
        Keys.require(key);
        final long now = clock.millis();
        final boolean[] allowed = new boolean[1];
        buckets.compute(key, (k, bucket) -> {
            double tokens;
            long last;
            if (bucket == null) {
                tokens = capacity;
                last = now;
            } else {
                last = Math.max(now, bucket.lastRefillMillis()); // tolerate clock going backwards
                long elapsed = Math.max(0L, now - bucket.lastRefillMillis());
                tokens = Math.min(capacity, bucket.tokens() + elapsed * refillPerMilli);
            }
            if (tokens >= 1.0) {
                tokens -= 1.0;
                allowed[0] = true;
            }
            return new Bucket(tokens, last);
        });
        return allowed[0];
    }
}
