package com.mag.gateway.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rate_limit_rules")
public class RuleEntity {

    @Id
    @Column(name = "route_id")
    private String routeId;
    @Column(name = "algorithm", nullable = false)
    private String algorithm;
    @Column(name = "max_requests", nullable = false)
    private int maxRequests;
    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    protected RuleEntity() { }

    public RuleEntity(String routeId, String algorithm, int maxRequests, int windowSeconds) {
        this.routeId = routeId;
        this.algorithm = algorithm;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public String getRouteId() { return routeId; }
    public String getAlgorithm() { return algorithm; }
    public int getMaxRequests() { return maxRequests; }
    public int getWindowSeconds() { return windowSeconds; }
}
