package com.mag.core.filter.impl;

import com.mag.core.auth.TokenVerifier;
import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;

import java.util.Objects;

/**
 * M-B: Identity &amp; Traffic Control
 *
 * SRP: validates the JWT bearer token only.
 * Delegates all token-specific knowledge to {@link TokenVerifier} (DIP).
 * On failure, short-circuits the chain with 401 (MAG-QR-05).
 * Claims are stored in the context for downstream filters (e.g., key extraction).
 */
public class AuthFilter implements GatewayFilter {

    private final TokenVerifier tokenVerifier;

    public AuthFilter(TokenVerifier tokenVerifier) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier);
    }

    @Override
    public void doFilter(GatewayContext ctx, FilterChain chain) {
        // TODO M2: extract Bearer token from ctx.getRequest(),
        //          call tokenVerifier.verify(token) — throws AuthenticationException on failure.
        //          Store verified claims in ctx attributes.
        //          On success, call chain.proceed(ctx).
        throw new UnsupportedOperationException("AuthFilter.doFilter — implement in M2");
    }
}
