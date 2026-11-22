package com.mag.core.filter.impl;

import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;
import com.mag.core.model.GatewayRequest;
import com.mag.core.model.GatewayResponse;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.util.Map;
import java.util.Objects;

/**
 * M-A: Proxy Pipeline
 *
 * SRP: forwards the request to the upstream via {@link UpstreamClient}
 * and writes the upstream's response back into the context.
 * This is always the last filter in the chain.
 *
 * The injected {@code UpstreamClient} is a decorator chain:
 *   CachingUpstreamClient → ResilientUpstreamClient → HTTP implementation
 * ForwardFilter is unaware of this chain (DIP / Proxy pattern); every client
 * implementation remains substitutable at this boundary (LSP).
 */
public class ForwardFilter implements GatewayFilter {

    private final UpstreamClient upstreamClient;

    public ForwardFilter(UpstreamClient upstreamClient) {
        this.upstreamClient = Objects.requireNonNull(upstreamClient);
    }

    @Override
    public void doFilter(GatewayContext ctx, FilterChain chain) {
        String requestId = ctx.getAttribute(RequestIdFilter.HEADER_REQUEST_ID);
        GatewayRequest request = requestWithRequestId(ctx.getRequest(), requestId);
        UpstreamResponse upstreamResponse = Objects.requireNonNull(
                upstreamClient.execute(request), "upstream response must not be null");

        GatewayResponse.Builder response = GatewayResponse.builder()
                .statusCode(upstreamResponse.getStatusCode())
                .body(upstreamResponse.getBody());
        for (Map.Entry<String, String> header : upstreamResponse.getHeaders().entrySet()) {
            if (!RequestIdFilter.HEADER_REQUEST_ID.equalsIgnoreCase(header.getKey())) {
                response.header(header.getKey(), header.getValue());
            }
        }
        if (requestId != null) {
            response.header(RequestIdFilter.HEADER_REQUEST_ID, requestId);
        }
        ctx.setResponse(response.build());
    }

    private GatewayRequest requestWithRequestId(GatewayRequest original, String requestId) {
        GatewayRequest.Builder request = GatewayRequest.builder()
                .method(original.getMethod())
                .path(original.getPath());
        for (Map.Entry<String, String> header : original.getHeaders().entrySet()) {
            if (!RequestIdFilter.HEADER_REQUEST_ID.equalsIgnoreCase(header.getKey())) {
                request.header(header.getKey(), header.getValue());
            }
        }
        if (requestId != null) {
            request.header(RequestIdFilter.HEADER_REQUEST_ID, requestId);
        }
        original.getQueryParams().forEach(request::queryParam);
        return request.build();
    }
}
