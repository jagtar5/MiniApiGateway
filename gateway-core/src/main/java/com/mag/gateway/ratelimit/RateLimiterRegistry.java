package com.mag.gateway.ratelimit;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds one live algorithm instance per route. Readers are lock-free (one volatile read of an
 * immutable map); {@link #replaceAll} swaps the whole map (copy-on-write), so a hot reload
 * never exposes a half-built state. Rules whose config did not change keep their instance, so
 * counters are NOT reset by an unrelated config edit.
 */
public final class RateLimiterRegistry {

    private record Entry(RateLimitConfig config, RateLimitAlgorithm algorithm) { }

    private final RateLimiterFactory factory;
    private final Clock clock;
    private final AtomicReference<Map<String, Entry>> entries = new AtomicReference<>(Map.of());

    public RateLimiterRegistry(RateLimiterFactory factory, Clock clock) {
        this.factory = java.util.Objects.requireNonNull(factory, "factory");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public Optional<RateLimitAlgorithm> forRoute(String routeId) {
        Entry e = entries.get().get(routeId);
        return e == null ? Optional.empty() : Optional.of(e.algorithm());
    }

    /** Replaces the rule set. Builds everything first; if any rule is invalid nothing changes. */
    public synchronized void replaceAll(Map<String, RateLimitConfig> rulesByRoute) {
        Map<String, Entry> old = entries.get();
        Map<String, Entry> next = new HashMap<>();
        rulesByRoute.forEach((routeId, cfg) -> {
            Entry prev = old.get(routeId);
            next.put(routeId, (prev != null && prev.config().equals(cfg))
                    ? prev
                    : new Entry(cfg, factory.create(cfg, clock)));
        });
        entries.set(Map.copyOf(next));
    }
}
