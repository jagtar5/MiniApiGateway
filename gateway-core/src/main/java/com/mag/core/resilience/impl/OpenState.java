package com.mag.core.resilience.impl;

import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.resilience.CircuitState;

import java.time.Instant;
import java.util.Objects;

/**
 * M-D: Resilience Engine — State / OPEN state.
 *
 * Fail-fast state: rejects all incoming requests immediately without calling upstream (MAG-QR-02).
 * After the configured open duration, allows a transition to HALF-OPEN
 * so trial probe calls can test upstream health.
 *
 * SRP: knows only the open-state transition rules.
 */
public class OpenState implements CircuitState {

    private final Instant openedAt;

    public OpenState(Instant openedAt) {
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
    }

    @Override
    public boolean allowRequest(CircuitBreaker ctx) {
        Instant threshold = openedAt.plus(ctx.getConfig().getOpenDuration());

        // Check if open duration has elapsed
        if (!ctx.getClock().now().isBefore(threshold)) {
            HalfOpenState halfOpen = new HalfOpenState(ctx.getConfig().getHalfOpenProbes());
            if (ctx.transitionTo(this, halfOpen)) {
                return halfOpen.allowRequest(ctx);
            }
            return ctx.allowRequest();
        }

        // Open duration has not yet elapsed; fail fast
        return false;
    }

    @Override
    public void onSuccess(CircuitBreaker ctx) {
        // No-op in OPEN (no requests forwarded so success should not occur)
    }

    @Override
    public void onFailure(CircuitBreaker ctx) {
        // No-op in OPEN (requests are rejected before reaching the upstream)
    }

    @Override
    public String getName() {
        return "OPEN";
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
