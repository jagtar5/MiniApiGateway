package com.mag.core.resilience.impl;

import com.mag.core.exception.CircuitOpenException;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.resilience.CircuitState;

import java.time.Instant;

/**
 * M-D: Resilience Engine — State / OPEN state
 *
 * Fail-fast state: rejects all requests with {@link CircuitOpenException} (503).
 * After the configured open duration, allows a transition to HALF-OPEN
 * so probe calls can test upstream health (MAG-QR-02).
 *
 * SRP: knows only the open-state transition rules.
 */
public class OpenState implements CircuitState {

    private final Instant openedAt;

    public OpenState(Instant openedAt) {
        this.openedAt = openedAt;
    }

    @Override
    public boolean allowRequest(CircuitBreaker ctx) {
        // TODO M2: if clock.now() is after openedAt + openDuration, transition to HalfOpenState; return false.
        throw new UnsupportedOperationException("OpenState.allowRequest — implement in M2");
    }

    @Override
    public void onSuccess(CircuitBreaker ctx) {
        // No-op in OPEN (no requests are forwarded so success shouldn't occur)
    }

    @Override
    public void onFailure(CircuitBreaker ctx) {
        // No-op in OPEN (requests are rejected before reaching the upstream)
    }
}
