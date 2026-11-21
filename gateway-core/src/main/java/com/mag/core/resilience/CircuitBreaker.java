package com.mag.core.resilience;

import com.mag.core.clock.Clock;
import com.mag.core.resilience.impl.ClosedState;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * M-D: Resilience Engine — State pattern context.
 *
 * Holds the current {@link CircuitState} and delegates all operational decisions to it.
 * State transitions happen atomically via compare-and-set to ensure that
 * concurrent failure/success reports trigger exactly one transition (MAG-QR-02).
 *
 * Time is decoupled via the injected {@link Clock} (MAG-QR-10).
 */
public final class CircuitBreaker {

    private final Clock clock;
    private final BreakerConfig config;

    /** Atomic reference ensuring atomic state transitions via CAS. */
    private final AtomicReference<CircuitState> state;

    public CircuitBreaker(Clock clock, BreakerConfig config) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.config = Objects.requireNonNull(config, "config");
        this.state = new AtomicReference<>(new ClosedState());
    }

    /**
     * Determines whether the next request may proceed. Delegates to the current state.
     *
     * @return true if permitted, false if rejected fast
     */
    public boolean allowRequest() {
        return state.get().allowRequest(this);
    }

    /**
     * Report a successful upstream call. Delegates to the current state.
     */
    public void onSuccess() {
        state.get().onSuccess(this);
    }

    /**
     * Report a failed upstream call. Delegates to the current state.
     */
    public void onFailure() {
        state.get().onFailure(this);
    }

    /**
     * Atomically transition to a new state using compare-and-set (CAS).
     *
     * @param expected the expected current state
     * @param next the target next state
     * @return true if the transition succeeded, false if another thread already transitioned
     */
    public boolean transitionTo(CircuitState expected, CircuitState next) {
        return state.compareAndSet(expected, next);
    }

    public Clock getClock() {
        return clock;
    }

    public BreakerConfig getConfig() {
        return config;
    }

    public CircuitState getState() {
        return state.get();
    }

    public String getStateName() {
        CircuitState current = state.get();
        return current != null ? current.getName() : "CLOSED";
    }
}
