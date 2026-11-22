package com.mag.gateway.config.web;

import com.mag.gateway.config.AdminCredentialChecker;
import com.mag.gateway.config.AdminService;
import com.mag.gateway.config.ConfigManager;
import com.mag.gateway.config.ConfigStore;
import com.mag.gateway.config.ConfigValidator;
import com.mag.gateway.config.RateLimiterRegistryRefresher;
import com.mag.gateway.ratelimit.RateLimiterFactory;
import com.mag.gateway.ratelimit.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wiring for M-E. If Jagtar's shell already defines some of these beans (P1-5), keep one
 * definition of each. The admin token is read from the environment (MAG_ADMIN_TOKEN) or
 * application property mag.admin.token - never hard-code it.
 */
@Configuration
public class AdminConfiguration {

    @Bean
    RateLimiterFactory rateLimiterFactory() {
        return RateLimiterFactory.withDefaults();
    }

    @Bean
    RateLimiterRegistry rateLimiterRegistry(RateLimiterFactory factory) {
        return new RateLimiterRegistry(factory, Clock.systemUTC());
    }

    @Bean
    ConfigManager configManager(RateLimiterFactory factory, RateLimiterRegistry registry, ConfigStore store) {
        ConfigManager manager = new ConfigManager(new ConfigValidator(factory::algorithmNames));
        manager.addListener(new RateLimiterRegistryRefresher(registry));
        manager.reloadFrom(store); // load persisted config at startup; invalid config fails startup loudly
        return manager;
    }

    @Bean
    AdminService adminService(ConfigManager manager, ConfigStore store) {
        return new AdminService(manager, store);
    }

    @Bean
    FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(@Value("${mag.admin.token:}") String token) {
        FilterRegistrationBean<AdminAuthFilter> bean =
                new FilterRegistrationBean<>(new AdminAuthFilter(new AdminCredentialChecker(token)));
        bean.addUrlPatterns("/admin/*");
        return bean;
    }
}
