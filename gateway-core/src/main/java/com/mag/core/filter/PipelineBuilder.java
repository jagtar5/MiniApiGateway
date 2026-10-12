package com.mag.core.filter;

import com.mag.core.config.RouteConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * M-A: Proxy Pipeline
 *
 * Builder pattern: constructs a {@link FilterChain} for a specific route.
 * Each route can enable or disable individual filters (e.g., auth-only
 * routes skip rate limiting; public routes skip auth).
 *
 * Open/Closed: adding a new filter type requires adding one builder method
 * and one class; no existing methods change.
 */
public final class PipelineBuilder {

    private final RouteConfig routeConfig;
    private final List<GatewayFilter> filters = new ArrayList<>();

    public PipelineBuilder(RouteConfig routeConfig) {
        this.routeConfig = Objects.requireNonNull(routeConfig);
    }

    /**
     * Append the request-ID stamping filter to the chain.
     * Should always be first so downstream filters can log the ID.
     */
    public PipelineBuilder withRequestId(GatewayFilter requestIdFilter) {
        filters.add(requestIdFilter);
        return this;
    }

    /**
     * Append JWT authentication (if the route requires auth).
     */
    public PipelineBuilder withAuth(GatewayFilter authFilter) {
        if (routeConfig.isAuthRequired()) {
            filters.add(authFilter);
        }
        return this;
    }

    /**
     * Append rate limiting.
     */
    public PipelineBuilder withRateLimit(GatewayFilter rateLimitFilter) {
        filters.add(rateLimitFilter);
        return this;
    }

    /**
     * Append the forwarding (and optional caching) filter.
     * Must be last in the chain.
     */
    public PipelineBuilder withForward(GatewayFilter forwardFilter) {
        filters.add(forwardFilter);
        return this;
    }

    /**
     * Build and return an immutable {@link FilterChain}.
     */
    public FilterChain build() {
        return new FilterChain(List.copyOf(filters));
    }
}
