package com.mag.core.resilience;

import java.time.Duration;

/**
 * M-D: Resilience Engine
 *
 * Immutable configuration for a {@link CircuitBreaker}.
 * Created once at startup from database/config; never mutated after creation.
 */
public final class BreakerConfig {

    /** Number of failures in the counting window that opens the circuit. */
    private final int failureThreshold;

    /** Duration the circuit stays OPEN before moving to HALF-OPEN. */
    private final Duration openDuration;

    /** Number of probe calls allowed in HALF-OPEN before deciding to close/re-open. */
    private final int halfOpenProbes;

    public BreakerConfig(int failureThreshold, Duration openDuration, int halfOpenProbes) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.halfOpenProbes = halfOpenProbes;
    }

    public int getFailureThreshold() { return failureThreshold; }
    public Duration getOpenDuration() { return openDuration; }
    public int getHalfOpenProbes() { return halfOpenProbes; }
}
