package com.mag.core.resilience.impl;

import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.resilience.CircuitState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * M-D: Resilience Engine — State / HALF-OPEN state.
 *
 * Probe state: allows a limited number of trial requests to test upstream recovery.
 *   - On success: once the required number of probe calls succeed, transitions back to CLOSED (MAG-QR-02).
 *   - On failure: any probe failure immediately trips the circuit back to OPEN.
 *
 * Thread-safety:
 *   - Probe allowance and success counts are tracked atomically using {@link AtomicInteger}.
 *
 * SRP: knows only the half-open-state transition rules.
 */
public class HalfOpenState implements CircuitState {

    private final int probesAllowed;
    private final AtomicInteger probesRemaining;
    private final AtomicInteger consecutiveSuccesses;

    public HalfOpenState(int probesAllowed) {
        if (probesAllowed <= 0) {
            throw new IllegalArgumentException("probesAllowed must be > 0, got: " + probesAllowed);
        }
        this.probesAllowed = probesAllowed;
        this.probesRemaining = new AtomicInteger(probesAllowed);
        this.consecutiveSuccesses = new AtomicInteger(0);
    }

    @Override
    public boolean allowRequest(CircuitBreaker ctx) {
        return probesRemaining.getAndDecrement() > 0;
    }

    @Override
    public void onSuccess(CircuitBreaker ctx) {
        if (consecutiveSuccesses.incrementAndGet() >= probesAllowed) {
            ctx.transitionTo(this, new ClosedState());
        }
    }

    @Override
    public void onFailure(CircuitBreaker ctx) {
        ctx.transitionTo(this, new OpenState(ctx.getClock().now()));
    }

    @Override
    public String getName() {
        return "HALF_OPEN";
    }

    public int getProbesAllowed() {
        return probesAllowed;
    }

    public int getProbesRemaining() {
        return probesRemaining.get();
    }

    public int getConsecutiveSuccesses() {
        return consecutiveSuccesses.get();
    }
}
