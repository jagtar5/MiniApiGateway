package com.mag.core.filter.impl;

import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;

import java.util.UUID;

/**
 * M-A: Proxy Pipeline
 *
 * SRP: stamps every incoming request with a unique X-Request-Id header.
 * Must be the first filter in every chain so all subsequent filters
 * and log statements can reference the same correlation ID.
 */
public class RequestIdFilter implements GatewayFilter {

    /** Header name written into the request context and forwarded upstream. */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public void doFilter(GatewayContext ctx, FilterChain chain) {
        // TODO M2: write a UUID into ctx attributes under HEADER_REQUEST_ID
        //          so downstream filters and the ForwardFilter can attach it.
        throw new UnsupportedOperationException("RequestIdFilter.doFilter — implement in M2");
    }
}
