package com.mag.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a request entering the gateway.
 * Immutability supports thread safety and prevents filters from mutating
 * the original request data (ISP / encapsulation, MAG-QR-06).
 *
 * The Spring shell converts a {@code HttpServletRequest} into this object
 * at the application boundary; the core never imports servlet types.
 */
public final class GatewayRequest {

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;

    private GatewayRequest(Builder builder) {
        this.method = Objects.requireNonNull(builder.method, "method");
        this.path = Objects.requireNonNull(builder.path, "path");
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.queryParams = Collections.unmodifiableMap(new HashMap<>(builder.queryParams));
    }

    public String getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String name) { return headers.get(name); }
    public Map<String, String> getQueryParams() { return queryParams; }

    // ---- Builder ---------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String method;
        private String path;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();

        public Builder method(String method) { this.method = method; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder header(String name, String value) { headers.put(name, value); return this; }
        public Builder queryParam(String name, String value) { queryParams.put(name, value); return this; }

        public GatewayRequest build() { return new GatewayRequest(this); }
    }
}
