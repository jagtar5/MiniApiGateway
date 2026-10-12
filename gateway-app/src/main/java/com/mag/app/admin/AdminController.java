package com.mag.app.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * M-E: Configuration &amp; Admin — Admin REST API
 *
 * Provides runtime management endpoints for routes and rate-limit rules.
 * All endpoints require the admin credential (MAG-QR-07).
 *
 * SRP: only handles admin HTTP concerns — delegates validation and
 * persistence to core interfaces (ConfigValidator, ConfigRepository, ConfigPublisher).
 *
 * Error responses name the offending field and reason (MAG-QR-13).
 *
 * TODO M2: inject ConfigValidator, ConfigRepository, ConfigPublisher via constructor.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    // ---- Routes ----------------------------------------------------------

    /**
     * GET /admin/routes — list all routes (requires admin credential).
     * MAG-QR-07: secured by admin credential check (TODO M2: add security filter).
     */
    @GetMapping("/routes")
    public ResponseEntity<?> listRoutes() {
        // TODO M2: return configProvider.getSnapshot().getRoutes() as JSON
        throw new UnsupportedOperationException("AdminController.listRoutes — implement in M2");
    }

    /**
     * POST /admin/routes — create or update a route.
     * Validates payload; returns 4xx with field name and reason on failure (MAG-QR-13).
     */
    @PostMapping("/routes")
    public ResponseEntity<?> upsertRoute(@RequestBody Object routePayload) {
        // TODO M2: validate, build RouteConfig, rebuild snapshot, publish atomically
        throw new UnsupportedOperationException("AdminController.upsertRoute — implement in M2");
    }

    /**
     * DELETE /admin/routes/{id} — remove a route.
     */
    @DeleteMapping("/routes/{id}")
    public ResponseEntity<?> deleteRoute(@PathVariable String id) {
        throw new UnsupportedOperationException("AdminController.deleteRoute — implement in M2");
    }

    // ---- Rate-limit rules ------------------------------------------------

    /**
     * GET /admin/routes/{routeId}/rules — list rate-limit rules for a route.
     */
    @GetMapping("/routes/{routeId}/rules")
    public ResponseEntity<?> listRules(@PathVariable String routeId) {
        throw new UnsupportedOperationException("AdminController.listRules — implement in M2");
    }

    /**
     * PUT /admin/routes/{routeId}/rules/{ruleId} — update a rate-limit rule.
     */
    @PutMapping("/routes/{routeId}/rules/{ruleId}")
    public ResponseEntity<?> updateRule(@PathVariable String routeId,
                                        @PathVariable String ruleId,
                                        @RequestBody Object rulePayload) {
        throw new UnsupportedOperationException("AdminController.updateRule — implement in M2");
    }

    // ---- Metrics (read-only) ---------------------------------------------

    /**
     * GET /admin/metrics — live metrics for the dashboard (MAG-FR in scope).
     * Returns: allowed/blocked counts, cache hit ratio, circuit state.
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> metrics() {
        throw new UnsupportedOperationException("AdminController.metrics — implement in M2");
    }
}
