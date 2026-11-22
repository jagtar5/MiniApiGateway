package com.mag.gateway.ratelimit;

import java.time.Clock;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory with a registry (Factory pattern + Open/Closed Principle).
 * Adding an algorithm = one new class + one {@link #register} call; no existing class is edited.
 */
public final class RateLimiterFactory {

    /** Builds one algorithm instance from a rule. */
    @FunctionalInterface
    public interface Creator {
        RateLimitAlgorithm create(RateLimitConfig config, Clock clock);
    }

    public static final String TOKEN_BUCKET = "token-bucket";
    public static final String FIXED_WINDOW = "fixed-window";
    public static final String SLIDING_WINDOW = "sliding-window";

    private final Map<String, Creator> registry = new ConcurrentHashMap<>();

    /** Empty factory (used by the OCP test). */
    public RateLimiterFactory() { }

    /** Factory with the three built-in algorithms registered. */
    public static RateLimiterFactory withDefaults() {
        RateLimiterFactory f = new RateLimiterFactory();
        f.register(TOKEN_BUCKET, (c, clock) -> new TokenBucketAlgorithm(c.limit(), c.window(), clock));
        f.register(FIXED_WINDOW, (c, clock) -> new FixedWindowAlgorithm(c.limit(), c.window(), clock));
        f.register(SLIDING_WINDOW, (c, clock) -> new SlidingWindowCounterAlgorithm(c.limit(), c.window(), clock));
        return f;
    }

    public void register(String name, Creator creator) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("algorithm name must not be blank");
        }
        if (creator == null) {
            throw new IllegalArgumentException("creator must not be null");
        }
        if (registry.putIfAbsent(normalise(name), creator) != null) {
            throw new IllegalStateException("algorithm already registered: " + name);
        }
    }

    public RateLimitAlgorithm create(RateLimitConfig config, Clock clock) {
        Creator creator = registry.get(normalise(config.algorithm()));
        if (creator == null) {
            throw new IllegalArgumentException("unknown rate-limit algorithm: " + config.algorithm());
        }
        return creator.create(config, clock);
    }

    public boolean isRegistered(String name) {
        return name != null && registry.containsKey(normalise(name));
    }

    public Set<String> algorithmNames() {
        return Collections.unmodifiableSet(new TreeSet<>(registry.keySet()));
    }

    private static String normalise(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
