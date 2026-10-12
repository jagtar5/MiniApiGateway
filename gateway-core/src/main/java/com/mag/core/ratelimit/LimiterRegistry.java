package com.mag.core.ratelimit;

import java.util.Map;

/**
 * M-B: Identity &amp; Traffic Control
 *
 * Holds all active per-route {@link RateLimitAlgorithm} instances.
 * The registry is rebuilt (or updated) on each config hot-reload.
 *
 * Thread-safety: implementations must allow concurrent reads and
 * atomic writes during reload (e.g., replacing the inner map atomically).
 */
public interface LimiterRegistry {

    /**
     * Return the algorithm configured for the given route ID,
     * or {@code null} if the route has no rate limit.
     *
     * @param routeId the route's database identifier
     */
    RateLimitAlgorithm getForRoute(String routeId);

    /**
     * Atomically replace all algorithms with a fresh set built from config.
     * Called by the config manager after a successful reload.
     *
     * @param algorithms new mapping of routeId → algorithm
     */
    void reload(Map<String, RateLimitAlgorithm> algorithms);
}
