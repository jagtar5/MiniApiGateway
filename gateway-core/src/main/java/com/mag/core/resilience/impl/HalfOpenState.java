package com.mag.core.resilience.impl;

import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.resilience.CircuitState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * M-D: Resilience Engine — State / HALF-OPEN state
 *
 * Probe state: allows a limited number of trial requests.
 *   - On success: if enough probes succeed, transitions back to CLOSED.
 *   - On failure: immediately returns to OPEN.
 *
 * Probe count is tracked with an {@link AtomicInteger} for thread safety (MAG-QR-02).
 *
 * SRP: knows only the half-open-state transition rules.
 */
public class HalfOpenState implements CircuitState {

    private final AtomicInteger probesRemaining;

    public HalfOpenState(int probesAllowed) {
        this.probesRemaining = new AtomicInteger(probesAllowed);
    }

    @Override
    public boolean allowRequest(CircuitBreaker ctx) {
        // TODO M2: return true if probesRemaining > 0; decrement atomically.
        throw new UnsupportedOperationException("HalfOpenState.allowRequest — implement in M2");
    }

    @Override
    public void onSuccess(CircuitBreaker ctx) {
        // TODO M2: transition to ClosedState if all probes succeeded.
        throw new UnsupportedOperationException("HalfOpenState.onSuccess — implement in M2");
    }

    @Override
    public void onFailure(CircuitBreaker ctx) {
        // TODO M2: transition back to OpenState immediately.
        throw new UnsupportedOperationException("HalfOpenState.onFailure — implement in M2");
    }
}
