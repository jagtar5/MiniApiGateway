package com.mag.app.config;

import com.mag.app.upstream.SimpleHttpUpstreamClient;
import com.mag.core.clock.Clock;
import com.mag.core.clock.SystemClock;
import com.mag.core.config.ConfigSnapshot;
import com.mag.core.config.RouteConfig;
import com.mag.core.resilience.BreakerConfig;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.impl.CachingUpstreamClient;
import com.mag.core.upstream.impl.ResilientUpstreamClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Spring wiring for infrastructure beans shared across modules.
 *
 * Decorator chain wiring (Proxy pattern):
 *   CachingUpstreamClient (M-C)
 *     → ResilientUpstreamClient (M-D)
 *       → SimpleHttpUpstreamClient (HTTP forwarder to dummy-upstreams on port 9090)
 */
@Configuration
public class InfrastructureConfig {

    @Value("${mag.upstream.default-base-url:http://localhost:9090}")
    private String upstreamBaseUrl;

    @Value("${mag.upstream.connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${mag.upstream.read-timeout-ms:5000}")
    private long readTimeoutMs;

    @Value("${mag.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${mag.circuit-breaker.open-duration-seconds:30}")
    private long openDurationSeconds;

    @Value("${mag.circuit-breaker.half-open-probes:3}")
    private int halfOpenProbes;

    @Bean
    public Clock clock() {
        return new SystemClock();
    }

    @Bean
    public AtomicConfigStore configStore() {
        AtomicConfigStore store = new AtomicConfigStore();

        // Seed with default routes for dummy upstream integration
        List<RouteConfig> seedRoutes = List.of(
                RouteConfig.builder()
                        .id("route-fast")
                        .pathPattern("/api/fast")
                        .upstreamUrls(List.of(upstreamBaseUrl + "/fast"))
                        .methods(List.of("GET"))
                        .authRequired(false)
                        .cacheEnabled(false)
                        .cacheTtlSeconds(0)
                        .priority(10)
                        .build(),
                RouteConfig.builder()
                        .id("route-cacheable")
                        .pathPattern("/api/cacheable")
                        .upstreamUrls(List.of(upstreamBaseUrl + "/cacheable"))
                        .methods(List.of("GET"))
                        .authRequired(false)
                        .cacheEnabled(true)
                        .cacheTtlSeconds(60)
                        .priority(10)
                        .build(),
                RouteConfig.builder()
                        .id("route-flaky")
                        .pathPattern("/api/flaky")
                        .upstreamUrls(List.of(upstreamBaseUrl + "/flaky"))
                        .methods(List.of("GET"))
                        .authRequired(false)
                        .cacheEnabled(false)
                        .cacheTtlSeconds(0)
                        .priority(10)
                        .build(),
                RouteConfig.builder()
                        .id("route-slow")
                        .pathPattern("/api/slow")
                        .upstreamUrls(List.of(upstreamBaseUrl + "/slow"))
                        .methods(List.of("GET"))
                        .authRequired(false)
                        .cacheEnabled(false)
                        .cacheTtlSeconds(0)
                        .priority(10)
                        .build(),
                RouteConfig.builder()
                        .id("route-items")
                        .pathPattern("/api/items/**")
                        .upstreamUrls(List.of(upstreamBaseUrl + "/fast"))
                        .methods(List.of("GET", "POST"))
                        .authRequired(true)
                        .cacheEnabled(true)
                        .cacheTtlSeconds(30)
                        .priority(5)
                        .build()
        );

        store.publish(new ConfigSnapshot(1L, seedRoutes));
        return store;
    }

    @Bean
    public BreakerConfig breakerConfig() {
        return new BreakerConfig(failureThreshold, Duration.ofSeconds(openDurationSeconds), halfOpenProbes);
    }

    @Bean
    public CircuitBreaker circuitBreaker(Clock clock, BreakerConfig breakerConfig) {
        return new CircuitBreaker(clock, breakerConfig);
    }

    @Bean
    public SimpleHttpUpstreamClient simpleHttpUpstreamClient() {
        return new SimpleHttpUpstreamClient(
                upstreamBaseUrl,
                Duration.ofMillis(connectTimeoutMs),
                Duration.ofMillis(readTimeoutMs)
        );
    }

    @Bean
    public ResilientUpstreamClient resilientUpstreamClient(
            SimpleHttpUpstreamClient httpUpstreamClient,
            CircuitBreaker circuitBreaker) {
        return new ResilientUpstreamClient(httpUpstreamClient, circuitBreaker);
    }

    @Bean
    public CachingUpstreamClient cachingUpstreamClient(
            ResilientUpstreamClient resilientUpstreamClient,
            Clock clock) {
        return new CachingUpstreamClient(resilientUpstreamClient, clock);
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public UpstreamClient upstreamClient(CachingUpstreamClient cachingUpstreamClient) {
        // The top-level UpstreamClient is the caching client
        return cachingUpstreamClient;
    }
}
