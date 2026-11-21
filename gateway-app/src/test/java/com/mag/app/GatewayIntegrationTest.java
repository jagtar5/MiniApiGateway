package com.mag.app;

import com.mag.core.resilience.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
class GatewayIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreaker circuitBreaker;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer change-me-before-production");
        return headers;
    }

    @Test
    @DisplayName("Demo dashboard static files are served correctly (MAG-FR-Demo)")
    void testDashboardStaticFiles() {
        ResponseEntity<String> htmlResponse = restTemplate.getForEntity(getBaseUrl() + "/index.html", String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        assertTrue(htmlResponse.getBody().contains("MAG Demo Dashboard"));

        ResponseEntity<String> jsResponse = restTemplate.getForEntity(getBaseUrl() + "/dashboard.js", String.class);
        assertEquals(HttpStatus.OK, jsResponse.getStatusCode());
        assertTrue(jsResponse.getBody().contains("refreshMetrics"));
    }

    @Test
    @DisplayName("Admin metrics require authorization credential (MAG-QR-07)")
    void testAdminMetricsSecurity() {
        // Without auth -> 401
        ResponseEntity<Map> unauth = restTemplate.getForEntity(getBaseUrl() + "/admin/metrics", Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, unauth.getStatusCode());

        // With auth -> 200 OK
        HttpEntity<?> entity = new HttpEntity<>(createAdminHeaders());
        ResponseEntity<Map> auth = restTemplate.exchange(getBaseUrl() + "/admin/metrics", HttpMethod.GET, entity, Map.class);
        assertEquals(HttpStatus.OK, auth.getStatusCode());
        assertNotNull(auth.getBody());
        assertTrue(auth.getBody().containsKey("circuitState"));
        assertTrue(auth.getBody().containsKey("allowedRequests"));
    }

    @Test
    @DisplayName("Admin routes API lists and updates active route rules (MAG-QR-07, MAG-QR-13)")
    void testAdminRoutesListingAndUpdating() {
        HttpEntity<?> getEntity = new HttpEntity<>(createAdminHeaders());
        ResponseEntity<List> routesResp = restTemplate.exchange(getBaseUrl() + "/admin/routes", HttpMethod.GET, getEntity, List.class);
        assertEquals(HttpStatus.OK, routesResp.getStatusCode());
        assertNotNull(routesResp.getBody());
        assertFalse(routesResp.getBody().isEmpty(), "Default seeded routes should be present");

        // Update cache TTL for route-cacheable via PUT
        Map<String, Object> updatePayload = Map.of("cacheTtlSeconds", 45, "cacheEnabled", true);
        HttpEntity<Map<String, Object>> putEntity = new HttpEntity<>(updatePayload, createAdminHeaders());
        ResponseEntity<Map> putResp = restTemplate.exchange(
                getBaseUrl() + "/admin/routes/route-cacheable",
                HttpMethod.PUT,
                putEntity,
                Map.class
        );
        assertEquals(HttpStatus.OK, putResp.getStatusCode());
        assertEquals(45, ((Number) putResp.getBody().get("cacheTtlSeconds")).intValue());
    }

    @Test
    @DisplayName("Gateway route with authRequired returns 401 when token is missing")
    void testAuthRequiredRejectsMissingToken() {
        // Route /api/items/** requires auth
        ResponseEntity<String> response = restTemplate.getForEntity(getBaseUrl() + "/api/items/123", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("401 Unauthorized"));
    }

    @Test
    @DisplayName("Circuit breaker fail-fast: circuit in OPEN state blocks requests with 503 (MAG-QR-02)")
    void testCircuitBreakerFailsFastInGateway() {
        // Manually trip breaker to OPEN to test gateway response mapping
        circuitBreaker.onFailure();
        circuitBreaker.onFailure();
        circuitBreaker.onFailure();
        circuitBreaker.onFailure();
        circuitBreaker.onFailure();
        assertEquals("OPEN", circuitBreaker.getStateName());

        ResponseEntity<String> response = restTemplate.getForEntity(getBaseUrl() + "/api/fast", String.class);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("503 Service Unavailable"));
    }
}
