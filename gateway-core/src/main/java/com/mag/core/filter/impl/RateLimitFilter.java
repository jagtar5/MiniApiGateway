package com.mag.core.filter.impl;

import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;
import com.mag.core.ratelimit.KeyExtractor;
import com.mag.core.ratelimit.LimiterRegistry;

import java.util.Objects;

/**
 * M-B: Identity &amp; Traffic Control
 *
 * SRP: enforces rate limits for the current client key only.
 * Delegates algorithm selection to {@link LimiterRegistry} and
 * client identification to {@link KeyExtractor} (Strategy pattern, DIP).
 * On limit exceeded, short-circuits the chain with 429 + Retry-After (MAG-FR-02).
 */
public class RateLimitFilter implements GatewayFilter {

    private final LimiterRegistry limiterRegistry;
    private final KeyExtractor keyExtractor;

    public RateLimitFilter(LimiterRegistry limiterRegistry, KeyExtractor keyExtractor) {
        this.limiterRegistry = Objects.requireNonNull(limiterRegistry);
        this.keyExtractor = Objects.requireNonNull(keyExtractor);
    }

    @Override
    public void doFilter(GatewayContext ctx, FilterChain chain) {
        // TODO M2: extract client key via keyExtractor.extract(ctx),
        //          find the relevant algorithm from limiterRegistry,
        //          call algorithm.tryAcquire(key) — short-circuit with 429 on DENIED.
        //          On ALLOWED, call chain.proceed(ctx).
        throw new UnsupportedOperationException("RateLimitFilter.doFilter — implement in M2");
    }
}
