package com.mag.core.ratelimit.algorithm;

import com.mag.core.filter.GatewayContext;
import com.mag.core.ratelimit.KeyExtractor;

/**
 * M-B: Identity &amp; Traffic Control — Strategy / Key Extraction
 *
 * Extracts the client's IP address as the rate-limit key.
 * Used for unauthenticated routes or as a fallback.
 */
public class ClientIpKeyExtractor implements KeyExtractor {

    /** Header added by load balancers / proxies. Checked before the direct IP. */
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public String extract(GatewayContext ctx) {
        // TODO M2: prefer X-Forwarded-For; fall back to request remote address.
        throw new UnsupportedOperationException("ClientIpKeyExtractor.extract — implement in M2");
    }
}
