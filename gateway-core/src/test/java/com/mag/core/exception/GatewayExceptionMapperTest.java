package com.mag.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayExceptionMapperTest {

    @Test
    void mapsAuthenticationExceptionTo401() {
        assertHttpStatus(new AuthenticationException("unauthorized"), 401);
    }

    @Test
    void mapsAuthorizationExceptionTo403() {
        assertHttpStatus(new AuthorizationException("forbidden"), 403);
    }

    @Test
    void mapsCircuitOpenExceptionTo503() {
        assertHttpStatus(new CircuitOpenException(), 503);
    }

    @Test
    void mapsInvalidConfigurationExceptionTo400() {
        assertHttpStatus(new InvalidConfigurationException("route", "invalid"), 400);
    }

    @Test
    void mapsRateLimitExceededExceptionTo429() {
        assertHttpStatus(new RateLimitExceededException(12), 429);
    }

    @Test
    void mapsUpstreamTimeoutExceptionTo504() {
        assertHttpStatus(new UpstreamTimeoutException("timed out"), 504);
    }

    @Test
    void mapsUpstreamUnavailableExceptionTo502() {
        assertHttpStatus(new UpstreamUnavailableException("unavailable"), 502);
    }

    @Test
    void retainsRateLimitRetryAfterHeader() {
        assertEquals("12", GatewayExceptionMapper.toResponse(new RateLimitExceededException(12))
                .getHeader("Retry-After"));
    }

    private void assertHttpStatus(GatewayException exception, int expectedStatus) {
        assertEquals(expectedStatus, GatewayExceptionMapper.toResponse(exception).getStatusCode());
    }
}
