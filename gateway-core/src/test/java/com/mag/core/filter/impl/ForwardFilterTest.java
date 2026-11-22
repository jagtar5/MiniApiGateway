package com.mag.core.filter.impl;

import com.mag.core.config.RouteConfig;
import com.mag.core.exception.UpstreamTimeoutException;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;
import com.mag.core.filter.PipelineBuilder;
import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForwardFilterTest {

    @Test
    void forwardsRequestIdAndCopiesUpstreamResponseWithoutContinuing() {
        UpstreamClient upstream = mock(UpstreamClient.class);
        when(upstream.execute(any())).thenReturn(UpstreamResponse.builder()
                .statusCode(201)
                .header("Content-Type", "text/plain")
                .body("created")
                .build());
        AtomicBoolean continued = new AtomicBoolean();
        GatewayFilter afterForward = (ctx, chain) -> continued.set(true);
        GatewayContext context = new GatewayContext(GatewayRequest.builder()
                .method("POST")
                .path("/resource")
                .header("X-Original", "preserved")
                .build());
        RouteConfig route = RouteConfig.builder().id("test").pathPattern("/resource").build();

        new PipelineBuilder(route)
                .withRequestId(new RequestIdFilter())
                .withForward(new ForwardFilter(upstream))
                .withRateLimit(afterForward)
                .build()
                .proceed(context);

        verify(upstream, times(1)).execute(any());
        org.mockito.ArgumentCaptor<GatewayRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(GatewayRequest.class);
        verify(upstream).execute(requestCaptor.capture());
        assertEquals("preserved", requestCaptor.getValue().getHeader("X-Original"));
        assertEquals(context.getAttribute(RequestIdFilter.HEADER_REQUEST_ID),
                requestCaptor.getValue().getHeader(RequestIdFilter.HEADER_REQUEST_ID));
        assertEquals(201, context.getResponse().getStatusCode());
        assertEquals("created", context.getResponse().getBody());
        assertEquals("text/plain", context.getResponse().getHeader("Content-Type"));
        assertEquals(context.getAttribute(RequestIdFilter.HEADER_REQUEST_ID),
                context.getResponse().getHeader(RequestIdFilter.HEADER_REQUEST_ID));
        assertFalse(continued.get());
    }

    @Test
    void propagatesUpstreamTimeoutForCentralExceptionMapping() {
        UpstreamClient upstream = mock(UpstreamClient.class);
        when(upstream.execute(any())).thenThrow(new UpstreamTimeoutException("timed out"));
        GatewayContext context = new GatewayContext(GatewayRequest.builder()
                .method("GET")
                .path("/resource")
                .build());
        RouteConfig route = RouteConfig.builder().id("test").pathPattern("/resource").build();

        assertThrows(UpstreamTimeoutException.class, () -> new PipelineBuilder(route)
                .withRequestId(new RequestIdFilter())
                .withForward(new ForwardFilter(upstream))
                .build()
                .proceed(context));
    }
}
