package com.mag.app.config;

import com.mag.core.clock.Clock;
import com.mag.core.clock.SystemClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for infrastructure beans shared across modules.
 *
 * DIP: core classes declare {@link Clock} as a constructor parameter.
 * This class provides the production implementation; tests inject {@code FakeClock}.
 *
 * TODO M2: add beans for RateLimiterFactory registrations, CircuitBreaker, etc.
 */
@Configuration
public class InfrastructureConfig {

    /**
     * Production clock — delegates to {@link java.time.Instant#now()}.
     */
    @Bean
    public Clock clock() {
        return new SystemClock();
    }
}
