package com.mag.core.config;

import com.mag.core.exception.InvalidConfigurationException;

import java.util.List;
import java.util.Objects;

/**
 * M-E: Configuration &amp; Admin
 *
 * Immutable per-route configuration record.
 * Corresponds to one row in the {@code routes} database table.
 */
public final class RouteConfig {

    private final String id;
    private final String pathPattern;
    private final List<String> upstreamUrls;
    private final List<String> methods;
    private final boolean authRequired;
    private final boolean cacheEnabled;
    private final long cacheTtlSeconds;
    private final int priority;

    private RouteConfig(Builder builder) {
        this.id = required(builder.id, "id");
        this.pathPattern = required(builder.pathPattern, "pathPattern");
        if (!pathPattern.startsWith("/")) {
            throw new InvalidConfigurationException("pathPattern", "must start with '/'");
        }
        this.upstreamUrls = copyList(builder.upstreamUrls, "upstreamUrls");
        this.methods = copyList(builder.methods, "methods");
        if (builder.cacheTtlSeconds < 0) {
            throw new InvalidConfigurationException("cacheTtlSeconds", "must not be negative");
        }
        this.authRequired = builder.authRequired;
        this.cacheEnabled = builder.cacheEnabled;
        this.cacheTtlSeconds = builder.cacheTtlSeconds;
        this.priority = builder.priority;
    }

    public String getId() { return id; }
    public String getPathPattern() { return pathPattern; }
    public List<String> getUpstreamUrls() { return upstreamUrls; }
    public List<String> getMethods() { return methods; }
    public boolean isAuthRequired() { return authRequired; }
    public boolean isCacheEnabled() { return cacheEnabled; }
    public long getCacheTtlSeconds() { return cacheTtlSeconds; }
    public int getPriority() { return priority; }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidConfigurationException(fieldName, "must not be blank");
        }
        return value;
    }

    private static List<String> copyList(List<String> values, String fieldName) {
        if (values == null || values.stream().anyMatch(Objects::isNull)) {
            throw new InvalidConfigurationException(fieldName, "must not be null or contain null values");
        }
        return List.copyOf(values);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id;
        private String pathPattern;
        private List<String> upstreamUrls = List.of();
        private List<String> methods = List.of();
        private boolean authRequired;
        private boolean cacheEnabled;
        private long cacheTtlSeconds;
        private int priority;

        public Builder id(String id) { this.id = id; return this; }
        public Builder pathPattern(String p) { this.pathPattern = p; return this; }
        public Builder upstreamUrls(List<String> u) { this.upstreamUrls = u; return this; }
        public Builder methods(List<String> m) { this.methods = m; return this; }
        public Builder authRequired(boolean b) { this.authRequired = b; return this; }
        public Builder cacheEnabled(boolean b) { this.cacheEnabled = b; return this; }
        public Builder cacheTtlSeconds(long s) { this.cacheTtlSeconds = s; return this; }
        public Builder priority(int p) { this.priority = p; return this; }
        public RouteConfig build() { return new RouteConfig(this); }
    }
}
