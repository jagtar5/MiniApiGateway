package com.mag.core.filter.impl;

import com.mag.core.config.RouteConfig;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;
import com.mag.core.filter.PipelineBuilder;
import com.mag.core.model.GatewayRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    @Test
    void setsRequestIdInContextUnderHeaderKey() {
        GatewayContext context = new GatewayContext(request());

        new PipelineBuilder(route()).withRequestId(new RequestIdFilter()).build().proceed(context);

        String requestId = context.getAttribute(RequestIdFilter.HEADER_REQUEST_ID);
        assertNotNull(requestId);
        UUID.fromString(requestId);
    }

    @Test
    void generatesUniqueRequestIdsForSeparateRequests() {
        RequestIdFilter requestIdFilter = new RequestIdFilter();
        GatewayContext first = new GatewayContext(request());
        GatewayContext second = new GatewayContext(request());

        new PipelineBuilder(route()).withRequestId(requestIdFilter).build().proceed(first);
        new PipelineBuilder(route()).withRequestId(requestIdFilter).build().proceed(second);

        String firstRequestId = (String) first.getAttribute(RequestIdFilter.HEADER_REQUEST_ID);
        String secondRequestId = (String) second.getAttribute(RequestIdFilter.HEADER_REQUEST_ID);
        assertNotEquals(firstRequestId, secondRequestId);
    }

    @Test
    void passesControlToTheNextFilter() {
        GatewayContext context = new GatewayContext(request());
        AtomicBoolean continued = new AtomicBoolean();
        GatewayFilter observer = (ctx, chain) -> {
            assertNotNull(ctx.getAttribute(RequestIdFilter.HEADER_REQUEST_ID));
            continued.set(true);
        };

        new PipelineBuilder(route())
                .withRequestId(new RequestIdFilter())
                .withForward(observer)
                .build()
                .proceed(context);

        assertTrue(continued.get());
    }

    private RouteConfig route() {
        return RouteConfig.builder().id("test").pathPattern("/").build();
    }

    private GatewayRequest request() {
        return GatewayRequest.builder().method("GET").path("/").build();
    }
}
