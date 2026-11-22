package com.mag.gateway.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mag.gateway.ratelimit.RateLimiterFactory;
import com.mag.gateway.ratelimit.RateLimiterRegistry;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class RateLimiterRegistryRefresherTest {

    @Test
    void configSwapRebuildsTheRegistry() {
        RateLimiterFactory factory = RateLimiterFactory.withDefaults();
        RateLimiterRegistry registry = new RateLimiterRegistry(factory, Clock.systemUTC());
        ConfigManager manager = new ConfigManager(new ConfigValidator(factory::algorithmNames));
        manager.addListener(new RateLimiterRegistryRefresher(registry));

        assertFalse(registry.forRoute("fast").isPresent());
        manager.reload(new ConfigSnapshot(0, List.of(ConfigValidatorTest.route("fast")),
                List.of(new RateLimitRuleConfig("fast", "fixed-window", 1, 60))));

        assertTrue(registry.forRoute("fast").isPresent());
        assertTrue(registry.forRoute("fast").get().tryAcquire("k"));
        assertFalse(registry.forRoute("fast").get().tryAcquire("k"));
    }
}
