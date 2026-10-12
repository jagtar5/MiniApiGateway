package com.mag.core.resilience;

import com.mag.core.clock.Clock;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * M-D: Resilience Engine — State pattern context.
 *
 * Holds the current {@link CircuitState} and delegates all decisions to it.
 * State transitions happen atomically via compare-and-set to avoid
 * concurrent success/failure reports causing multiple transitions (MAG-QR-02).
 *
 * Time comes from the injected {@link Clock} (MAG-QR-10).
 */
public final class CircuitBreaker {

    private final Clock clock;
    private final BreakerConfig config;

    /** Atomic so that concurrent onSuccess/onFailure calls produce exactly one transition. */
    private final AtomicReference<CircuitState> state;

    public CircuitBreaker(Clock clock, BreakerConfig config) {
        this.clock = Objects.requireNonNull(clock);
        this.config = Objects.requireNonNull(config);
        // TODO M2: initialise state to new ClosedState()
        this.state = new AtomicReference<>(null /* replace with ClosedState */);
    }

    /** Ask whether the next request may proceed. Delegates to the current state. */
    public boolean allowRequest() {
        return state.get().allowRequest(this);
    }

    /** Report a successful upstream call. Delegates to the current state. */
    public void onSuccess() {
        state.get().onSuccess(this);
    }

    /** Report a failed upstream call. Delegates to the current state. */
    public void onFailure() {
        state.get().onFailure(this);
    }

    /**
     * Atomically transition to a new state (CAS).
     * Called by state implementations during transition; never by external callers.
     */
    public void transitionTo(CircuitState expected, CircuitState next) {
        state.compareAndSet(expected, next);
    }

    public Clock getClock() { return clock; }
    public BreakerConfig getConfig() { return config; }
    public CircuitState getState() { return state.get(); }
}
