package com.mag.gateway.ratelimit;

import java.util.Optional;

/**
 * The decision logic of the rate-limit step, free of any web/framework types so it is
 * unit-testable. RateLimitFilter is a thin adapter around this class.
 */
public final class RateLimitGuard {

    public record Result(boolean allowed, boolean ruleApplied, String key) { }

    private static final String ANONYMOUS = "anonymous";

    private final KeyExtractor primary;
    private final KeyExtractor fallback;
    private final RateLimiterRegistry registry;

    public RateLimitGuard(KeyExtractor primary, KeyExtractor fallback, RateLimiterRegistry registry) {
        this.primary = java.util.Objects.requireNonNull(primary, "primary");
        this.fallback = fallback;
        this.registry = java.util.Objects.requireNonNull(registry, "registry");
    }

    public Result check(RequestView request, String routeId) {
        Optional<RateLimitAlgorithm> algorithm = registry.forRoute(routeId);
        if (algorithm.isEmpty()) {
            return new Result(true, false, null); // no rule configured for this route
        }
        String key = primary.extract(request);
        if (key == null && fallback != null) {
            key = fallback.extract(request);
        }
        if (key == null) {
            key = ANONYMOUS; // shared bucket: unidentifiable callers cannot dodge the limit
        }
        return new Result(algorithm.get().tryAcquire(key), true, key);
    }
}
