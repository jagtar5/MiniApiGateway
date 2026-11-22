package com.mag.gateway.config;

/** The rate-limit rule of one route (at most one rule per route). Immutable. */
public record RateLimitRuleConfig(String routeId, String algorithm, int limit, int windowSeconds) { }
