package com.mag.core.filter;

import com.mag.core.model.GatewayRequest;
import com.mag.core.model.GatewayResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * M-A: Proxy Pipeline
 *
 * Mutable context object that travels through the {@link FilterChain}.
 * Each filter reads from {@code request} (read-only) and may write
 * to {@code response} or to typed attributes.
 *
 * Calling {@link #shortCircuit(GatewayResponse)} stops chain execution
 * immediately without calling any subsequent filter.
 */
public final class GatewayContext {

    /** Immutable snapshot of the incoming request. */
    private final GatewayRequest request;

    /** Response built up by filters; initially null. */
    private GatewayResponse response;

    /** Typed bag of cross-filter attributes (e.g., parsed JWT claims). */
    private final Map<String, Object> attributes = new HashMap<>();

    /** True once shortCircuit() has been called. */
    private boolean shortCircuited;

    public GatewayContext(GatewayRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    // ---- Read-only request access ----------------------------------------

    public GatewayRequest getRequest() {
        return request;
    }

    // ---- Response management ---------------------------------------------

    public GatewayResponse getResponse() {
        return response;
    }

    public void setResponse(GatewayResponse response) {
        this.response = response;
    }

    /**
     * Set a terminal response and halt the filter chain.
     * After this call {@link FilterChain#proceed} is a no-op.
     *
     * @param response the response to return to the client (e.g., 401, 429, 503)
     */
    public void shortCircuit(GatewayResponse response) {
        this.response = response;
        this.shortCircuited = true;
    }

    public boolean isShortCircuited() {
        return shortCircuited;
    }

    // ---- Cross-filter attribute bag ---------------------------------------

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}
