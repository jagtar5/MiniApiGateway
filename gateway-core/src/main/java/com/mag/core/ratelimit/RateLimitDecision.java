package com.mag.core.ratelimit;

/**
 * Immutable result of a {@link RateLimitAlgorithm#tryAcquire} call.
 *
 * Immutability prevents filters from mutating decision state
 * and supports thread-safe sharing across threads.
 */
public final class RateLimitDecision {

    public enum Outcome { ALLOWED, DENIED }

    private final Outcome outcome;
    private final long retryAfterSeconds; // 0 when ALLOWED

    private RateLimitDecision(Outcome outcome, long retryAfterSeconds) {
        this.outcome = outcome;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static RateLimitDecision allowed() {
        return new RateLimitDecision(Outcome.ALLOWED, 0);
    }

    public static RateLimitDecision denied(long retryAfterSeconds) {
        return new RateLimitDecision(Outcome.DENIED, retryAfterSeconds);
    }

    public boolean isAllowed() { return outcome == Outcome.ALLOWED; }
    public boolean isDenied()  { return outcome == Outcome.DENIED;  }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
