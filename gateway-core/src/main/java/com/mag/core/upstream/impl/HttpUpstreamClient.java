package com.mag.core.upstream.impl;

import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.time.Duration;
import java.util.Objects;

/**
 * M-A: Proxy Pipeline — actual HTTP client.
 *
 * SRP: makes real HTTP calls only. Has no knowledge of caching or circuit breaking.
 * Connect and read timeouts enforce the deadline required by MAG-QR-03.
 *
 * The Spring shell provides the underlying HTTP client implementation;
 * the core declares only the timeout configuration.
 */
public class HttpUpstreamClient implements UpstreamClient {

    private final Duration connectTimeout;
    private final Duration readTimeout;

    // TODO M2: inject actual HTTP client (e.g., java.net.http.HttpClient) via constructor

    public HttpUpstreamClient(Duration connectTimeout, Duration readTimeout) {
        this.connectTimeout = Objects.requireNonNull(connectTimeout);
        this.readTimeout = Objects.requireNonNull(readTimeout);
    }

    @Override
    public UpstreamResponse execute(GatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        // P0-1 interface lock: this core skeleton is intentionally not a real
        // network implementation. A 501 response keeps callers deterministic
        // until P1-1 replaces it with the real HTTP implementation.
        return UpstreamResponse.builder()
                .statusCode(501)
                .header("Content-Type", "text/plain")
                .body("Not Implemented")
                .build();
    }
}
