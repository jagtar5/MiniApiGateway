package com.mag.core.ratelimit;

import com.mag.core.filter.GatewayContext;

/**
 * M-B: Identity &amp; Traffic Control — Strategy pattern
 *
 * Extracts the client-identifying key from the request context.
 * Different routes may use different strategies (JWT subject vs. IP).
 *
 * ISP: a single method so the filter need not import auth or IP-specific classes.
 */
public interface KeyExtractor {

    /**
     * Extract a stable, non-null client identifier from the context.
     *
     * @param ctx the current request context (JWT claims are stored in attributes)
     * @return a non-null string used as the rate-limit key
     */
    String extract(GatewayContext ctx);
}
