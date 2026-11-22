package com.mag.gateway.config;

import com.mag.gateway.ratelimit.RateLimitConfig;
import com.mag.gateway.ratelimit.RateLimiterRegistry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Glue: after every config swap, rebuilds the rate-limit rules in the RateLimiterRegistry.
 * Register with {@code configManager.addListener(new RateLimiterRegistryRefresher(registry))}
 * and call it once at startup with the initial snapshot.
 */
public final class RateLimiterRegistryRefresher implements Consumer<ConfigSnapshot> {

    private final RateLimiterRegistry registry;

    public RateLimiterRegistryRefresher(RateLimiterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void accept(ConfigSnapshot snapshot) {
        Map<String, RateLimitConfig> rules = new HashMap<>();
        for (RateLimitRuleConfig r : snapshot.rules()) {
            rules.put(r.routeId(), new RateLimitConfig(r.algorithm(), r.limit(), Duration.ofSeconds(r.windowSeconds())));
        }
        registry.replaceAll(rules);
    }
}
