package com.mag.core.config;

import java.util.List;
import java.util.Optional;

/**
 * M-E: Configuration &amp; Admin — DIP
 *
 * Repository abstraction for loading routes and rate-limit rules from the database.
 * The Spring shell provides the JPA/JDBC implementation; the core depends only
 * on this interface (DIP — no Spring or JDBC imports in gateway-core).
 */
public interface ConfigRepository {

    /**
     * Load all active routes ordered by priority (highest first).
     *
     * @return ordered list of route configurations
     */
    List<RouteConfig> findAllRoutes();

    /**
     * Load the most-recently active (last-known-good) configuration snapshot.
     * Returns empty if no valid snapshot has been recorded yet.
     *
     * @return the last-known-good snapshot, or empty
     */
    Optional<ConfigSnapshot> findLastKnownGood();

    /**
     * Persist a new validated snapshot as the active version.
     *
     * @param snapshot the validated snapshot to store
     */
    void saveSnapshot(ConfigSnapshot snapshot);
}
