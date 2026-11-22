package com.mag.core.config;

import java.util.List;
import java.util.Objects;

/**
 * M-E: Configuration &amp; Admin
 *
 * Immutable snapshot of all active routes and rules.
 * Immutability + {@code AtomicReference} in the Spring shell enable
 * lock-free hot reload without dropping any in-flight request (MAG-QR-04).
 *
 * Thread-safety: all fields are final; collections are unmodifiable.
 * The shell swaps the {@code AtomicReference} to this new object atomically.
 */
public record ConfigSnapshot(long version, List<RouteConfig> routes) {

    /**
     * Defensive copy at the boundary keeps the snapshot immutable even if a
     * caller reuses or mutates a source list after publication (DIP/SRP).
     */
    public ConfigSnapshot {
        Objects.requireNonNull(routes, "routes must not be null");
        routes = List.copyOf(routes);
    }

    // Compatibility accessors for the Spring shell.
    public long getVersion() {
        return version;
    }

    public List<RouteConfig> getRoutes() {
        return routes;
    }
}
