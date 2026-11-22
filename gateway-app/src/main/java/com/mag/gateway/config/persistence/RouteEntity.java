package com.mag.gateway.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Spring Boot 3 / Jakarta Persistence. On Boot 2 change the imports to javax.persistence. */
@Entity
@Table(name = "routes")
public class RouteEntity {

    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "path_pattern", nullable = false)
    private String pathPattern;
    @Column(name = "upstream_url", nullable = false)
    private String upstreamUrl;
    @Column(name = "cache_ttl_seconds", nullable = false)
    private int cacheTtlSeconds;
    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;

    protected RouteEntity() { }

    public RouteEntity(String id, String pathPattern, String upstreamUrl, int cacheTtlSeconds, int timeoutMs) {
        this.id = id;
        this.pathPattern = pathPattern;
        this.upstreamUrl = upstreamUrl;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.timeoutMs = timeoutMs;
    }

    public String getId() { return id; }
    public String getPathPattern() { return pathPattern; }
    public String getUpstreamUrl() { return upstreamUrl; }
    public int getCacheTtlSeconds() { return cacheTtlSeconds; }
    public int getTimeoutMs() { return timeoutMs; }
}
