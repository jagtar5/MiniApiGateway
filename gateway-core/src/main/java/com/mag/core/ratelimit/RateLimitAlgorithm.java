package com.mag.core.ratelimit;

/**
 * M-B: Identity &amp; Traffic Control — Strategy pattern
 *
 * A single, pluggable rate-limiting algorithm.
 * Implementations: {@code TokenBucket}, {@code FixedWindow}, {@code SlidingWindowCounter}.
 *
 * Open/Closed (MAG-QR-11): adding a new algorithm requires one new class
 * that implements this interface plus one registration in {@link RateLimiterFactory}.
 * No existing algorithm class is modified.
 *
 * Liskov Substitution: all implementations must satisfy the parametrized
 * contract test (never exceed limit, never negative, no exception on valid input).
 */
public interface RateLimitAlgorithm {

    /**
     * Attempt to acquire a token for the given client key.
     *
     * @param key the client-identifying string (e.g., JWT subject or IP)
     * @return a {@link RateLimitDecision} indicating ALLOWED or DENIED
     */
    RateLimitDecision tryAcquire(String key);
}
