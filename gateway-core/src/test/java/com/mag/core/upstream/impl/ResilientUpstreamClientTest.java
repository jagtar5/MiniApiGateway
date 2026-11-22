package com.mag.core.upstream.impl;

import com.mag.core.clock.FakeClock;
import com.mag.core.exception.CircuitOpenException;
import com.mag.core.exception.UpstreamTimeoutException;
import com.mag.core.exception.UpstreamUnavailableException;
import com.mag.core.model.GatewayRequest;
import com.mag.core.resilience.BreakerConfig;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResilientUpstreamClientTest {

    private UpstreamClient mockDelegate;
    private FakeClock fakeClock;
    private CircuitBreaker circuitBreaker;
    private ResilientUpstreamClient resilientClient;

    @BeforeEach
    void setUp() {
        mockDelegate = mock(UpstreamClient.class);
        fakeClock = new FakeClock(Instant.parse("2026-11-21T10:00:00Z"));
        // threshold = 2, openDuration = 20s, halfOpenProbes = 1
        BreakerConfig config = new BreakerConfig(2, Duration.ofSeconds(20), 1);
        circuitBreaker = new CircuitBreaker(fakeClock, config);
        resilientClient = new ResilientUpstreamClient(mockDelegate, circuitBreaker);
    }

    @Test
    @DisplayName("Normal execution delegates to upstream and records success")
    void testNormalExecution() {
        GatewayRequest request = GatewayRequest.builder().method("GET").path("/test").build();
        UpstreamResponse expected = UpstreamResponse.builder().statusCode(200).body("ok").build();
        when(mockDelegate.execute(request)).thenReturn(expected);

        UpstreamResponse response = resilientClient.execute(request);
        assertEquals(200, response.getStatusCode());
        assertEquals("ok", response.getBody());
        verify(mockDelegate, times(1)).execute(request);
        assertEquals("CLOSED", circuitBreaker.getState().getName());
    }

    @Test
    @DisplayName("Failures trip the circuit breaker; subsequent requests fail fast with CircuitOpenException (503) (MAG-QR-02)")
    void testTripCircuitBreakerFailsFast() {
        GatewayRequest request = GatewayRequest.builder().method("GET").path("/fail").build();

        when(mockDelegate.execute(request))
                .thenThrow(new UpstreamTimeoutException("timeout 1"))
                .thenThrow(new UpstreamTimeoutException("timeout 2"));

        // Call 1: fails
        assertThrows(UpstreamTimeoutException.class, () -> resilientClient.execute(request));
        assertEquals("CLOSED", circuitBreaker.getState().getName());

        // Call 2: fails -> threshold (2) reached -> trips to OPEN
        assertThrows(UpstreamTimeoutException.class, () -> resilientClient.execute(request));
        assertEquals("OPEN", circuitBreaker.getState().getName());

        // Call 3: circuit is OPEN -> fails fast with CircuitOpenException without calling delegate!
        CircuitOpenException thrown = assertThrows(CircuitOpenException.class, () -> resilientClient.execute(request));
        assertEquals(503, thrown.getHttpStatus());

        // Delegate should only have been called 2 times, never for the 3rd request
        verify(mockDelegate, times(2)).execute(request);
    }

    @Test
    @DisplayName("Circuit recovers via half-open probe on successful response")
    void testCircuitRecoversViaProbe() {
        GatewayRequest request = GatewayRequest.builder().method("GET").path("/service").build();

        when(mockDelegate.execute(request))
                .thenThrow(new UpstreamTimeoutException("err 1"))
                .thenThrow(new UpstreamTimeoutException("err 2"))
                .thenReturn(UpstreamResponse.builder().statusCode(200).body("recovered").build());

        // Trip to OPEN
        assertThrows(UpstreamTimeoutException.class, () -> resilientClient.execute(request));
        assertThrows(UpstreamTimeoutException.class, () -> resilientClient.execute(request));
        assertEquals("OPEN", circuitBreaker.getState().getName());

        // Advance fake clock past open duration (20s)
        fakeClock.advance(Duration.ofSeconds(21));

        // Next request is a trial probe allowed in HALF_OPEN
        UpstreamResponse probeResponse = resilientClient.execute(request);
        assertEquals(200, probeResponse.getStatusCode());
        assertEquals("recovered", probeResponse.getBody());

        // Circuit must now be back to CLOSED!
        assertEquals("CLOSED", circuitBreaker.getState().getName());

        // Subsequent call succeeds normally in CLOSED state
        when(mockDelegate.execute(request)).thenReturn(UpstreamResponse.builder().statusCode(200).body("normal").build());
        UpstreamResponse nextResponse = resilientClient.execute(request);
        assertEquals("normal", nextResponse.getBody());
    }

    @Test
    @DisplayName("Null upstream response is converted to 502 and records breaker failure")
    void testNullUpstreamResponse() {
        GatewayRequest request = GatewayRequest.builder().method("GET").path("/null").build();
        when(mockDelegate.execute(request)).thenReturn(null);

        assertThrows(UpstreamUnavailableException.class, () -> resilientClient.execute(request));
        assertEquals("CLOSED", circuitBreaker.getState().getName());
    }

    @Test
    @DisplayName("Upstream 5xx response records failure on circuit breaker")
    void testUpstream5xxRecordsFailure() {
        GatewayRequest request = GatewayRequest.builder().method("GET").path("/flaky").build();
        UpstreamResponse error500 = UpstreamResponse.builder().statusCode(500).body("Server Error").build();
        when(mockDelegate.execute(request)).thenReturn(error500);

        // Call 1
        resilientClient.execute(request);
        assertEquals("CLOSED", circuitBreaker.getState().getName());

        // Call 2: hits threshold (2) -> trips to OPEN
        resilientClient.execute(request);
        assertEquals("OPEN", circuitBreaker.getState().getName());

        // Call 3: rejected fast
        assertThrows(CircuitOpenException.class, () -> resilientClient.execute(request));
    }
}
