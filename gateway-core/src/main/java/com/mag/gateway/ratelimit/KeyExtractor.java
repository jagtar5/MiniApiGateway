package com.mag.gateway.ratelimit;

/** Decides WHO a request is for rate-limiting purposes (Strategy). */
public interface KeyExtractor {

    /** @return a non-blank key, or null if this extractor cannot identify the caller. */
    String extract(RequestView request);
}
