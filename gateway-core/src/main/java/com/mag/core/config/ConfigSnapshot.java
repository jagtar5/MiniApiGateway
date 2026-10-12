package com.mag.core.config;

import java.util.Collections;
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
public final class ConfigSnapshot {

    private final long version;
    private final List<RouteConfig> routes;

    public ConfigSnapshot(long version, List<RouteConfig> routes) {
        this.version = version;
        this.routes = Collections.unmodifiableList(Objects.requireNonNull(routes));
    }

    public long getVersion() { return version; }
    public List<RouteConfig> getRoutes() { return routes; }
}
