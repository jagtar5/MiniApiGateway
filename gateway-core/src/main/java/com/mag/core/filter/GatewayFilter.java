package com.mag.core.filter;

/**
 * M-A: Proxy Pipeline
 *
 * The single entry point for every stage in the request-processing chain.
 * Each concrete filter is responsible for exactly one concern (SRP).
 * Filters depend on this interface, never on each other (DIP).
 *
 * Chain of Responsibility pattern: each filter calls chain.proceed(ctx)
 * to pass control to the next filter, or calls ctx.shortCircuit(resp)
 * to stop processing immediately (e.g., failed authentication).
 */
public interface GatewayFilter {

    /**
     * Execute this filter's single responsibility.
     *
     * @param ctx   mutable request/response context for this request
     * @param chain the remainder of the filter chain; call proceed() to continue
     */
    void doFilter(GatewayContext ctx, FilterChain chain);
}
