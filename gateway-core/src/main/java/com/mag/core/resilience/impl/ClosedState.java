package com.mag.core.resilience.impl;

import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.resilience.CircuitState;

/**
 * M-D: Resilience Engine — State / CLOSED state
 *
 * Normal operating state.  Counts failures within a sliding window;
 * transitions to OPEN when failures reach the configured threshold.
 *
 * SRP: knows only the closed-state transition rules.
 */
public class ClosedState implements CircuitState {

    // TODO M2: FailureWindow inner class for counting failures within a time window

    @Override
    public boolean allowRequest(CircuitBreaker ctx) {
        return true; // CLOSED: all requests are allowed
    }

    @Override
    public void onSuccess(CircuitBreaker ctx) {
        // TODO M2: reset the failure counter on success (optional by config)
    }

    @Override
    public void onFailure(CircuitBreaker ctx) {
        // TODO M2: increment failure counter; if count >= threshold, ctx.transitionTo(this, new OpenState())
        throw new UnsupportedOperationException("ClosedState.onFailure — implement in M2");
    }
}
