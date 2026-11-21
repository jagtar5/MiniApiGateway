package com.mag.app.admin;

import com.mag.app.config.AtomicConfigStore;
import com.mag.app.metrics.GatewayMetrics;
import com.mag.core.config.ConfigSnapshot;
import com.mag.core.config.RouteConfig;
import com.mag.core.resilience.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * M-E: Configuration &amp; Admin — Admin REST API (Spring shell).
 *
 * Provides runtime management endpoints for routes, rules, and metrics.
 * Security: protected by admin credential header (MAG-QR-07).
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AtomicConfigStore configStore;
    private final CircuitBreaker circuitBreaker;
    private final GatewayMetrics metrics;

    @Value("${mag.admin.credential:change-me-before-production}")
    private String adminCredential;

    public AdminController(
            AtomicConfigStore configStore,
            CircuitBreaker circuitBreaker,
            GatewayMetrics metrics) {
        this.configStore = configStore;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return false;
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
        return adminCredential.equals(token);
    }

    // ---- Metrics ---------------------------------------------------------

    @GetMapping("/metrics")
    public ResponseEntity<?> metrics(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: valid admin credential required (MAG-QR-07)"));
        }

        return ResponseEntity.ok(Map.of(
                "allowedRequests", metrics.getAllowedRequests(),
                "blockedRequests", metrics.getBlockedRequests(),
                "cacheHits", metrics.getCacheHits(),
                "cacheMisses", metrics.getCacheMisses(),
                "cacheHitRatio", metrics.getCacheHitRatio(),
                "circuitState", circuitBreaker.getStateName()
        ));
    }

    // ---- Routes ----------------------------------------------------------

    @GetMapping("/routes")
    public ResponseEntity<?> listRoutes(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: valid admin credential required (MAG-QR-07)"));
        }

        ConfigSnapshot snapshot = configStore.getSnapshot();
        return ResponseEntity.ok(snapshot.getRoutes());
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<?> updateRoute(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: valid admin credential required (MAG-QR-07)"));
        }

        ConfigSnapshot currentSnapshot = configStore.getSnapshot();
        List<RouteConfig> updatedRoutes = new ArrayList<>();
        RouteConfig updated = null;

        for (RouteConfig route : currentSnapshot.getRoutes()) {
            if (route.getId().equals(id)) {
                long newTtl = payload.containsKey("cacheTtlSeconds")
                        ? ((Number) payload.get("cacheTtlSeconds")).longValue()
                        : route.getCacheTtlSeconds();
                boolean cacheEnabled = payload.containsKey("cacheEnabled")
                        ? (Boolean) payload.get("cacheEnabled")
                        : route.isCacheEnabled();

                updated = RouteConfig.builder()
                        .id(route.getId())
                        .pathPattern(route.getPathPattern())
                        .upstreamUrls(route.getUpstreamUrls())
                        .methods(route.getMethods())
                        .authRequired(route.isAuthRequired())
                        .cacheEnabled(cacheEnabled)
                        .cacheTtlSeconds(newTtl)
                        .priority(route.getPriority())
                        .build();
                updatedRoutes.add(updated);
            } else {
                updatedRoutes.add(route);
            }
        }

        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Route with ID " + id + " not found"));
        }

        // Publish new immutable snapshot atomically
        configStore.publish(new ConfigSnapshot(currentSnapshot.getVersion() + 1, updatedRoutes));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/routes")
    public ResponseEntity<?> createRoute(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: valid admin credential required (MAG-QR-07)"));
        }

        // Field validation (MAG-QR-13)
        if (!payload.containsKey("id") || !payload.containsKey("pathPattern")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation error: 'id' and 'pathPattern' are required (MAG-QR-13)"));
        }

        RouteConfig newRoute = RouteConfig.builder()
                .id((String) payload.get("id"))
                .pathPattern((String) payload.get("pathPattern"))
                .upstreamUrls(payload.containsKey("upstreamUrls") ? (List<String>) payload.get("upstreamUrls") : List.of())
                .methods(payload.containsKey("methods") ? (List<String>) payload.get("methods") : List.of("GET"))
                .authRequired(payload.containsKey("authRequired") && (Boolean) payload.get("authRequired"))
                .cacheEnabled(payload.containsKey("cacheEnabled") && (Boolean) payload.get("cacheEnabled"))
                .cacheTtlSeconds(payload.containsKey("cacheTtlSeconds") ? ((Number) payload.get("cacheTtlSeconds")).longValue() : 30)
                .priority(payload.containsKey("priority") ? ((Number) payload.get("priority")).intValue() : 10)
                .build();

        ConfigSnapshot current = configStore.getSnapshot();
        List<RouteConfig> list = new ArrayList<>(current.getRoutes());
        list.add(newRoute);
        configStore.publish(new ConfigSnapshot(current.getVersion() + 1, list));

        return ResponseEntity.status(HttpStatus.CREATED).body(newRoute);
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<?> deleteRoute(
            @PathVariable("id") String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: valid admin credential required (MAG-QR-07)"));
        }

        ConfigSnapshot current = configStore.getSnapshot();
        List<RouteConfig> remaining = current.getRoutes().stream()
                .filter(r -> !r.getId().equals(id))
                .toList();

        configStore.publish(new ConfigSnapshot(current.getVersion() + 1, remaining));
        return ResponseEntity.ok(Map.of("deleted", id));
    }
}
