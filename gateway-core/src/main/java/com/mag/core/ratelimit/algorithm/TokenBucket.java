package com.mag.core.ratelimit.algorithm;

import com.mag.core.clock.Clock;
import com.mag.core.ratelimit.RateLimitAlgorithm;
import com.mag.core.ratelimit.RateLimitDecision;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M-B: Identity &amp; Traffic Control — Strategy / Token Bucket algorithm.
 *
 * Refills tokens at a constant rate; bursts up to {@code capacity}.
 * Per-key state is updated atomically via {@link ConcurrentHashMap#compute}
 * to avoid check-then-act races (MAG-QR-01).
 *
 * Time comes from the injected {@link Clock}, never from {@code System.currentTimeMillis()}
 * so tests are deterministic (MAG-QR-10, R6).
 */
public class TokenBucket implements RateLimitAlgorithm {

    private final int capacity;
    private final double refillRatePerSecond;
    private final Clock clock;

    // TODO M2: define inner Bucket state class and ConcurrentHashMap<String, Bucket>

    public TokenBucket(int capacity, double refillRatePerSecond, Clock clock) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public RateLimitDecision tryAcquire(String key) {
        // TODO M2: atomically refill and consume one token for this key.
        throw new UnsupportedOperationException("TokenBucket.tryAcquire — implement in M2");
    }
}
