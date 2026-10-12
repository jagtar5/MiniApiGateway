package com.mag.core.filter.impl;

import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;
import com.mag.core.upstream.UpstreamClient;

import java.util.Objects;

/**
 * M-A: Proxy Pipeline
 *
 * SRP: forwards the request to the upstream via {@link UpstreamClient}
 * and writes the upstream's response back into the context.
 * This is always the last filter in the chain.
 *
 * The injected {@code UpstreamClient} is a decorator chain:
 *   CachingUpstreamClient → ResilientUpstreamClient → HttpUpstreamClient
 * ForwardFilter is unaware of this chain (DIP / Proxy pattern).
 */
public class ForwardFilter implements GatewayFilter {

    private final UpstreamClient upstreamClient;

    public ForwardFilter(UpstreamClient upstreamClient) {
        this.upstreamClient = Objects.requireNonNull(upstreamClient);
    }

    @Override
    public void doFilter(GatewayContext ctx, FilterChain chain) {
        // TODO M2: call upstreamClient.execute(ctx.getRequest()),
        //          write the UpstreamResponse into ctx.setResponse(...).
        //          Do NOT call chain.proceed — this is the terminal filter.
        throw new UnsupportedOperationException("ForwardFilter.doFilter — implement in M2");
    }
}
