package com.mag.gateway.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Audit trail: one row per successful save. */
@Entity
@Table(name = "config_versions")
public class ConfigVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "version", nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "route_count", nullable = false)
    private int routeCount;
    @Column(name = "rule_count", nullable = false)
    private int ruleCount;

    protected ConfigVersionEntity() { }

    public ConfigVersionEntity(long version, int routeCount, int ruleCount) {
        this.version = version;
        this.routeCount = routeCount;
        this.ruleCount = ruleCount;
    }
}
