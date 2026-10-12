package com.mag.core.upstream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable upstream HTTP response.
 * Immutability prevents the cache from accidentally sharing mutable state
 * between concurrent requests (MAG-QR-06).
 */
public final class UpstreamResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    private UpstreamResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body = builder.body;
    }

    public int getStatusCode() { return statusCode; }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String name) { return headers.get(name); }
    public String getBody() { return body; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int statusCode = 200;
        private final Map<String, String> headers = new HashMap<>();
        private String body = "";

        public Builder statusCode(int code) { this.statusCode = code; return this; }
        public Builder header(String name, String value) { headers.put(name, value); return this; }
        public Builder body(String body) { this.body = body; return this; }
        public UpstreamResponse build() { return new UpstreamResponse(this); }
    }
}
