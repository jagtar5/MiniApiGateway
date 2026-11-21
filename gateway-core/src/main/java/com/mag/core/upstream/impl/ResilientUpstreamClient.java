package com.mag.core.upstream.impl;

import com.mag.core.exception.CircuitOpenException;
import com.mag.core.exception.GatewayException;
import com.mag.core.exception.UpstreamUnavailableException;
import com.mag.core.model.GatewayRequest;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.util.Objects;

/**
 * M-D: Resilience Engine — Proxy pattern decorator around {@link UpstreamClient}.
 *
 * Responsibilities (SRP: resilience only):
 *   - Delegates requests through the {@link CircuitBreaker} (MAG-QR-02).
 *   - Fails fast with {@link CircuitOpenException} (HTTP 503) when the circuit is OPEN.
 *   - Reports success/failure back to the circuit breaker after each call.
 *   - When wrapped by {@link CachingUpstreamClient}, cache hits bypass this decorator entirely.
 */
public class ResilientUpstreamClient implements UpstreamClient {

    private final UpstreamClient delegate;
    private final CircuitBreaker circuitBreaker;

    public ResilientUpstreamClient(UpstreamClient delegate, CircuitBreaker circuitBreaker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker");
    }

    @Override
    public UpstreamResponse execute(GatewayRequest request) {
        Objects.requireNonNull(request, "request");

        // 1. Fail fast if circuit breaker rejects the call
        if (!circuitBreaker.allowRequest()) {
            throw new CircuitOpenException();
        }

        // 2. Delegate execution to underlying upstream client
        UpstreamResponse response;
        try {
            response = delegate.execute(request);
        } catch (RuntimeException t) {
            circuitBreaker.onFailure();
            if (t instanceof GatewayException ge) {
                throw ge;
            }
            throw t;
        } catch (Error t) {
            circuitBreaker.onFailure();
            throw t;
        }

        if (response == null) {
            circuitBreaker.onFailure();
            throw new UpstreamUnavailableException("Upstream returned a null response", null);
        }

        // 3. Status check: 5xx counts as upstream failure, < 500 counts as success
        if (response.getStatusCode() >= 500) {
            circuitBreaker.onFailure();
        } else {
            circuitBreaker.onSuccess();
        }

        return response;
    }

    public UpstreamClient getDelegate() {
        return delegate;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
