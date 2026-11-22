package com.mag.core.filter.impl;

import com.mag.core.clock.FakeClock;
import com.mag.core.config.RouteConfig;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.PipelineBuilder;
import com.mag.core.model.GatewayRequest;
import com.mag.core.resilience.BreakerConfig;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;
import com.mag.core.upstream.impl.CachingUpstreamClient;
import com.mag.core.upstream.impl.HttpUpstreamClient;
import com.mag.core.upstream.impl.ResilientUpstreamClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpstreamClientLspContractTest {

    private static final String RESPONSE_BODY = "upstream response";

    @ParameterizedTest(name = "{0} is substitutable through ForwardFilter")
    @MethodSource("upstreamClients")
    void forwardsTheResponseFromEveryUpstreamClient(
            String implementation,
            UpstreamClient upstreamClient,
            int expectedStatus,
            String expectedBody) {
        GatewayContext context = new GatewayContext(GatewayRequest.builder()
                .method("GET")
                .path("/contract")
                .build());
        RouteConfig route = RouteConfig.builder().id("contract").pathPattern("/contract").build();

        new PipelineBuilder(route)
                .withForward(new ForwardFilter(upstreamClient))
                .build()
                .proceed(context);

        assertNotNull(context.getResponse(), implementation);
        assertEquals(expectedStatus, context.getResponse().getStatusCode(), implementation);
        assertEquals(expectedBody, context.getResponse().getBody(), implementation);
    }

    private static Stream<Arguments> upstreamClients() {
        UpstreamClient delegate = request -> UpstreamResponse.builder()
                .statusCode(201)
                .body(RESPONSE_BODY)
                .build();
        FakeClock clock = new FakeClock(Instant.parse("2026-11-22T00:00:00Z"));
        CircuitBreaker breaker = new CircuitBreaker(
                clock, new BreakerConfig(3, Duration.ofSeconds(10), 1));

        return Stream.of(
                Arguments.of("caching decorator",
                        new CachingUpstreamClient(delegate, clock), 201, RESPONSE_BODY),
                Arguments.of("resilient decorator",
                        new ResilientUpstreamClient(delegate, breaker), 201, RESPONSE_BODY),
                Arguments.of("HTTP contract placeholder",
                        new HttpUpstreamClient(Duration.ofSeconds(1), Duration.ofSeconds(1)),
                        501, "Not Implemented"));
    }
}
