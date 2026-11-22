package com.mag.core.filter.impl;

import com.mag.core.auth.TokenVerifier;
import com.mag.core.config.RouteConfig;
import com.mag.core.exception.AuthenticationException;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.GatewayFilter;
import com.mag.core.filter.PipelineBuilder;
import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class AuthFilterTest {

    private static final String AUTHORIZATION = "Authorization";

    @Test
    void verifiesBearerTokenStoresClaimsAndContinuesTheChain() {
        TokenVerifier verifier = mock(TokenVerifier.class);
        when(verifier.verify("signed-token")).thenReturn(Map.of("sub", "client-7"));
        GatewayContext context = new GatewayContext(request("bEaReR signed-token"));
        AtomicBoolean continued = new AtomicBoolean();
        GatewayFilter observer = (ctx, chain) -> continued.set(true);

        new PipelineBuilder(route())
                .withAuth(new AuthFilter(verifier))
                .withForward(observer)
                .build()
                .proceed(context);

        assertEquals("client-7", context.getAttribute("jwt.sub"));
        assertEquals(Map.of("sub", "client-7"), context.getAttribute("jwt.claims"));
        org.junit.jupiter.api.Assertions.assertTrue(continued.get());
        verify(verifier).verify("signed-token");
    }

    @Test
    void missingOrMalformedBearerHeadersAreRejectedBeforeVerification() {
        TokenVerifier verifier = mock(TokenVerifier.class);
        AuthFilter filter = new AuthFilter(verifier);

        for (String authorization : new String[] {null, "", "Basic token", "Bearer ", "Bearer", "Bearer token extra"}) {
            GatewayContext context = new GatewayContext(request(authorization));
            assertThrows(AuthenticationException.class,
                    () -> new PipelineBuilder(route()).withAuth(filter).build().proceed(context));
        }

        GatewayRequest duplicateHeaders = GatewayRequest.builder()
                .method("GET")
                .path("/secure")
                .header("Authorization", "Bearer first")
                .header("authorization", "Bearer second")
                .build();
        assertThrows(AuthenticationException.class,
                () -> new PipelineBuilder(route()).withAuth(filter)
                        .build().proceed(new GatewayContext(duplicateHeaders)));
        verify(verifier, never()).verify(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void verifierRejectionsDoNotReachTheForwardFilter() {
        TokenVerifier verifier = mock(TokenVerifier.class);
        UpstreamClient upstream = mock(UpstreamClient.class);
        ForwardFilter forwardFilter = spy(new ForwardFilter(upstream));
        when(verifier.verify("expired-token")).thenThrow(new AuthenticationException("Invalid token"));
        GatewayContext context = new GatewayContext(request("Bearer expired-token"));

        assertThrows(AuthenticationException.class, () -> new PipelineBuilder(route())
                .withAuth(new AuthFilter(verifier))
                .withForward(forwardFilter)
                .build()
                .proceed(context));

        verify(forwardFilter, never()).doFilter(any(), any());
    }

    private RouteConfig route() {
        return RouteConfig.builder().id("secured").pathPattern("/secure").authRequired(true).build();
    }

    private GatewayRequest request(String authorization) {
        GatewayRequest.Builder builder = GatewayRequest.builder().method("GET").path("/secure");
        if (authorization != null) {
            builder.header(AUTHORIZATION, authorization);
        }
        return builder.build();
    }
}
