package com.mag.core.upstream.impl;

import com.mag.core.model.GatewayRequest;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.util.Objects;

/**
 * M-D: Resilience Engine — Proxy pattern decorator around {@link UpstreamClient}.
 *
 * Responsibilities (SRP: resilience only):
 *   - Delegate all calls through the {@link CircuitBreaker} (MAG-QR-02).
 *   - Fail fast with 503 when the circuit is OPEN (no upstream call made).
 *   - Report success/failure back to the breaker after each call.
 *   - Let the breaker enforce the timeout deadline (MAG-QR-03).
 *
 * The cache sits above this decorator, so cached hits never affect failure statistics.
 */
public class ResilientUpstreamClient implements UpstreamClient {

    private final UpstreamClient delegate;
    private final CircuitBreaker circuitBreaker;

    public ResilientUpstreamClient(UpstreamClient delegate, CircuitBreaker circuitBreaker) {
        this.delegate = Objects.requireNonNull(delegate);
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker);
    }

    @Override
    public UpstreamResponse execute(GatewayRequest request) {
        // TODO M2:
        //   1. Call circuitBreaker.allowRequest(); throw CircuitOpenException if false.
        //   2. Try delegate.execute(request).
        //   3. On success call circuitBreaker.onSuccess(); return response.
        //   4. On failure call circuitBreaker.onFailure(); rethrow as UpstreamUnavailableException.
        throw new UnsupportedOperationException("ResilientUpstreamClient.execute — implement in M2");
    }
}
