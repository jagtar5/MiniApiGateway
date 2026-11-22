package com.mag.core.filter.impl;

import com.mag.core.auth.TokenVerifier;
import com.mag.core.exception.AuthenticationException;
import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * M-B: Identity &amp; Traffic Control
 *
 * SRP: validates the JWT bearer token only.
 * Delegates all token-specific knowledge to {@link TokenVerifier} (DIP).
 * On failure, throws an authentication exception for central mapping (MAG-QR-05).
 * Claims are stored in the context for downstream filters (e.g., key extraction).
 */
public class AuthFilter implements GatewayFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CLAIMS_ATTRIBUTE = "jwt.claims";
    private static final String SUBJECT_ATTRIBUTE = "jwt.sub";

    private final TokenVerifier tokenVerifier;

    public AuthFilter(TokenVerifier tokenVerifier) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier);
    }

    @Override
    public void doFilter(GatewayContext ctx, FilterChain chain) {
        String authorization = null;
        boolean foundAuthorization = false;
        for (Map.Entry<String, String> header : ctx.getRequest().getHeaders().entrySet()) {
            if (AUTHORIZATION_HEADER.equalsIgnoreCase(header.getKey())) {
                if (foundAuthorization) {
                    throw new AuthenticationException("Multiple Authorization headers are not allowed");
                }
                foundAuthorization = true;
                authorization = header.getValue();
            }
        }

        if (!foundAuthorization || authorization == null) {
            throw new AuthenticationException("Bearer token is required");
        }

        String value = authorization.trim();
        int separator = value.indexOf(' ');
        if (separator <= 0 || !"Bearer".equalsIgnoreCase(value.substring(0, separator))) {
            throw new AuthenticationException("Authorization header must use Bearer");
        }

        String token = value.substring(separator + 1).trim();
        if (token.isEmpty() || containsWhitespace(token)) {
            throw new AuthenticationException("Bearer token is malformed");
        }

        Map<String, Object> verifiedClaims = tokenVerifier.verify(token);
        if (verifiedClaims == null) {
            throw new AuthenticationException("Bearer token claims are missing");
        }
        Map<String, Object> claims = Collections.unmodifiableMap(new HashMap<>(verifiedClaims));
        ctx.setAttribute(CLAIMS_ATTRIBUTE, claims);
        ctx.setAttribute(SUBJECT_ATTRIBUTE, claims.get("sub"));
        chain.proceed(ctx);
    }

    private boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }
}
