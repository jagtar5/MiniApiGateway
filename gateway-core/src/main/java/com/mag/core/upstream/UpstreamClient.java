package com.mag.core.upstream;

import com.mag.core.model.GatewayRequest;

/**
 * M-A / M-C / M-D: Core upstream abstraction (Proxy pattern)
 *
 * The single interface through which the filter chain calls upstream services.
 * Three implementations form a decorator chain:
 *
 *   {@link com.mag.core.upstream.impl.CachingUpstreamClient}  (M-C: cache + single-flight)
 *     → {@link com.mag.core.upstream.impl.ResilientUpstreamClient} (M-D: circuit breaker + timeout)
 *       → {@link com.mag.core.upstream.impl.HttpUpstreamClient}   (M-A: actual HTTP call)
 *
 * {@link com.mag.core.filter.impl.ForwardFilter} is unaware of this chain (DIP / Proxy pattern).
 * Swapping or removing a decorator requires zero changes to other classes (OCP).
 */
public interface UpstreamClient {

    /**
     * Execute the given request against the upstream.
     *
     * @param request the immutable gateway request
     * @return the upstream's response, never null
     * @throws com.mag.core.exception.CircuitOpenException       if the circuit is OPEN
     * @throws com.mag.core.exception.UpstreamTimeoutException   if the upstream hangs
     * @throws com.mag.core.exception.UpstreamUnavailableException on HTTP error
     */
    UpstreamResponse execute(GatewayRequest request);
}
