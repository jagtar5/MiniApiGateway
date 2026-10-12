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
        // TODO M2: build HTTP request from GatewayRequest, apply timeouts,
        //          forward X-Request-Id header, return UpstreamResponse.
        //          Throw UpstreamTimeoutException on timeout, UpstreamUnavailableException on error.
        throw new UnsupportedOperationException("HttpUpstreamClient.execute — implement in M2");
    }
}
