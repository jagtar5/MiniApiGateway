package com.mag.core.ratelimit.algorithm;

import com.mag.core.filter.GatewayContext;
import com.mag.core.ratelimit.KeyExtractor;

/**
 * M-B: Identity &amp; Traffic Control — Strategy / Key Extraction
 *
 * Extracts the JWT {@code sub} claim as the rate-limit key.
 * JWT claims must have been verified and stored in context attributes by {@code AuthFilter}.
 */
public class JwtSubjectKeyExtractor implements KeyExtractor {

    /** Attribute key under which AuthFilter stores the subject claim. */
    public static final String ATTR_JWT_SUBJECT = "jwt.sub";

    @Override
    public String extract(GatewayContext ctx) {
        // TODO M2: retrieve ATTR_JWT_SUBJECT from ctx.getAttribute(ATTR_JWT_SUBJECT).
        //          Throw AuthenticationException if absent (auth filter should have caught this).
        throw new UnsupportedOperationException("JwtSubjectKeyExtractor.extract — implement in M2");
    }
}
