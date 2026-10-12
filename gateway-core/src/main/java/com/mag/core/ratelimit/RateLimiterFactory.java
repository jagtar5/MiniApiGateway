package com.mag.core.ratelimit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M-B: Identity &amp; Traffic Control — Factory pattern
 *
 * Creates {@link RateLimitAlgorithm} instances from configuration.
 * Uses a registry map (algorithm-type → creator) so that:
 *   - callers never reference concrete algorithm classes (DIP),
 *   - a new algorithm is registered in one place with no edits to existing code (OCP, MAG-QR-11).
 *
 * The Spring shell registers all algorithm creators at startup
 * via {@link #register(String, AlgorithmCreator)}.
 */
public final class RateLimiterFactory {

    /** Function interface for creating an algorithm from a rule config map. */
    @FunctionalInterface
    public interface AlgorithmCreator {
        RateLimitAlgorithm create(Map<String, Object> ruleConfig);
    }

    private final Map<String, AlgorithmCreator> registry = new ConcurrentHashMap<>();

    /**
     * Register a creator for a named algorithm type.
     * Called once at application startup per algorithm.
     *
     * @param algorithmType the string name used in the database (e.g., "TOKEN_BUCKET")
     * @param creator       function that builds an instance from database-sourced config
     */
    public void register(String algorithmType, AlgorithmCreator creator) {
        registry.put(
                Objects.requireNonNull(algorithmType, "algorithmType").toUpperCase(),
                Objects.requireNonNull(creator, "creator")
        );
    }

    /**
     * Build a {@link RateLimitAlgorithm} from the given rule configuration.
     *
     * @param algorithmType the algorithm name (e.g., "TOKEN_BUCKET")
     * @param ruleConfig    the database row as a config map
     * @return a ready-to-use algorithm instance
     * @throws IllegalArgumentException if the type is not registered
     */
    public RateLimitAlgorithm create(String algorithmType, Map<String, Object> ruleConfig) {
        AlgorithmCreator creator = registry.get(algorithmType.toUpperCase());
        if (creator == null) {
            throw new IllegalArgumentException("Unknown rate-limit algorithm: " + algorithmType);
        }
        return creator.create(ruleConfig);
    }
}
