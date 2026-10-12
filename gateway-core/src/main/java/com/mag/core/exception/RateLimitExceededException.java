package com.mag.core.exception;

/** 429 Too Many Requests — client exceeded the configured rate limit (MAG-FR-02). */
public class RateLimitExceededException extends GatewayException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(429, "Rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
