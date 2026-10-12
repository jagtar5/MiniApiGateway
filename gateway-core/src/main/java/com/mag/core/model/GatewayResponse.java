package com.mag.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable gateway response written back to the client.
 * Created by individual filters (e.g., 401, 429) or by {@code ForwardFilter}
 * after a successful upstream call.
 */
public final class GatewayResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    private GatewayResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body = builder.body;
    }

    public int getStatusCode() { return statusCode; }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String name) { return headers.get(name); }
    public String getBody() { return body; }

    // ---- Factory helpers for common error responses ----------------------

    public static GatewayResponse unauthorized(String message) {
        return builder().statusCode(401).body(message).build();
    }

    public static GatewayResponse forbidden(String message) {
        return builder().statusCode(403).body(message).build();
    }

    public static GatewayResponse tooManyRequests(long retryAfterSeconds) {
        return builder()
                .statusCode(429)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body("Rate limit exceeded")
                .build();
    }

    public static GatewayResponse badGateway() {
        return builder().statusCode(502).body("Bad Gateway").build();
    }

    public static GatewayResponse serviceUnavailable() {
        return builder().statusCode(503).body("Service Unavailable").build();
    }

    public static GatewayResponse gatewayTimeout() {
        return builder().statusCode(504).body("Gateway Timeout").build();
    }

    // ---- Builder ---------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int statusCode = 200;
        private final Map<String, String> headers = new HashMap<>();
        private String body = "";

        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder header(String name, String value) { headers.put(name, value); return this; }
        public Builder body(String body) { this.body = body; return this; }

        public GatewayResponse build() { return new GatewayResponse(this); }
    }
}
