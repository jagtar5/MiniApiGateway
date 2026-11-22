package com.mag.gateway.ratelimit;

import java.time.Duration;

/** Immutable description of one rate-limit rule: algorithm name, max requests per window. */
public record RateLimitConfig(String algorithm, int limit, Duration window) {

    public RateLimitConfig {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm must not be blank");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be > 0");
        }
    }
}
