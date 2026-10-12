package com.mag.core.filter;

import java.util.List;

/**
 * M-A: Proxy Pipeline
 *
 * Holds the ordered list of {@link GatewayFilter}s and tracks the current
 * execution position.  Calling {@code proceed(ctx)} advances one step;
 * when the list is exhausted the method returns normally.
 *
 * Chain of Responsibility: the chain object travels alongside the context
 * so that each filter can decide independently whether to advance or halt.
 */
public final class FilterChain {

    private final List<GatewayFilter> filters;
    private int position;

    /**
     * Package-private: constructed only by {@link PipelineBuilder}.
     */
    FilterChain(List<GatewayFilter> filters) {
        this.filters = List.copyOf(filters); // immutable copy
        this.position = 0;
    }

    /**
     * Advance to the next filter, or return if the chain is exhausted.
     *
     * @param ctx the shared request/response context
     */
    public void proceed(GatewayContext ctx) {
        if (ctx.isShortCircuited() || position >= filters.size()) {
            return;
        }
        GatewayFilter next = filters.get(position++);
        next.doFilter(ctx, this);
    }
}
