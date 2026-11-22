package com.mag.gateway.config;

/** One proxied route. Immutable. Validation is done by {@link ConfigValidator}, not here. */
public record RouteConfig(String id, String pathPattern, String upstreamUrl,
                          int cacheTtlSeconds, int timeoutMs) { }
