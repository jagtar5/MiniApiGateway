package com.mag.gateway.ratelimit;

/**
 * Strategy interface for rate limiting (Strategy pattern, SOLID: O/L/I).
 *
 * <p>Contract (checked by RateLimitAlgorithmContractTest for every implementation):
 * <ul>
 *   <li>never throws for a non-blank key,</li>
 *   <li>never grants more than the configured limit inside one accounting period,</li>
 *   <li>is safe to call from many threads at once (atomic check-then-act per key).</li>
 * </ul>
 */
public interface RateLimitAlgorithm {

    /**
     * @param key identity being limited (user subject, client IP, ...)
     * @return true if the request may proceed, false if it must be rejected (HTTP 429)
     * @throws IllegalArgumentException if key is null or blank (programming error at the boundary)
     */
    boolean tryAcquire(String key);
}
