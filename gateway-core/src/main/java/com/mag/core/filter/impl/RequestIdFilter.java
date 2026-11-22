package com.mag.core.filter.impl;

import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;


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
        String requestId = UUID.randomUUID().toString();
        ctx.setAttribute(HEADER_REQUEST_ID, requestId);
        chain.proceed(ctx);
    }
}
