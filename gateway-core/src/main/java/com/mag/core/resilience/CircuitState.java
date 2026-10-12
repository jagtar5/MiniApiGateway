package com.mag.core.resilience;

/**
 * M-D: Resilience Engine — State pattern
 *
 * Defines the behaviour of the circuit breaker in a single state.
 * Each concrete state ({@link impl.ClosedState}, {@link impl.OpenState},
 * {@link impl.HalfOpenState}) implements this interface independently;
 * transition logic lives inside the state, not in a conditional block.
 *
 * ISP: three small methods, no unrelated concerns.
 */
public interface CircuitState {

    /**
     * @param ctx the owning circuit breaker (allows state transition via ctx.transitionTo)
     * @return true if the request may proceed to the upstream
     */
    boolean allowRequest(CircuitBreaker ctx);

    /**
     * Called after a successful upstream response.
     *
     * @param ctx the owning circuit breaker
     */
    void onSuccess(CircuitBreaker ctx);

    /**
     * Called after a failed upstream response (error, timeout, etc.).
     *
     * @param ctx the owning circuit breaker
     */
    void onFailure(CircuitBreaker ctx);
}
