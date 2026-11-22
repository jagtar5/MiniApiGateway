package com.mag.gateway.config.persistence;

import com.mag.gateway.config.ConfigSnapshot;
import com.mag.gateway.config.ConfigStore;
import com.mag.gateway.config.RateLimitRuleConfig;
import com.mag.gateway.config.RouteConfig;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** JPA-backed {@link ConfigStore}. save() runs in ONE transaction: all-or-nothing. */
@Repository
public class ConfigRepository implements ConfigStore {

    private final RouteJpaRepository routes;
    private final RuleJpaRepository rules;
    private final ConfigVersionJpaRepository versions;

    public ConfigRepository(RouteJpaRepository routes, RuleJpaRepository rules, ConfigVersionJpaRepository versions) {
        this.routes = routes;
        this.rules = rules;
        this.versions = versions;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigSnapshot load() {
        List<RouteConfig> r = routes.findAll().stream()
                .map(e -> new RouteConfig(e.getId(), e.getPathPattern(), e.getUpstreamUrl(),
                        e.getCacheTtlSeconds(), e.getTimeoutMs()))
                .toList();
        List<RateLimitRuleConfig> l = rules.findAll().stream()
                .map(e -> new RateLimitRuleConfig(e.getRouteId(), e.getAlgorithm(),
                        e.getMaxRequests(), e.getWindowSeconds()))
                .toList();
        return new ConfigSnapshot(versions.count(), r, l);
    }

    @Override
    @Transactional
    public void save(ConfigSnapshot s) {
        rules.deleteAllInBatch();          // children first (FK rules -> routes)
        routes.deleteAllInBatch();
        routes.saveAll(s.routes().stream()
                .map(r -> new RouteEntity(r.id(), r.pathPattern(), r.upstreamUrl(), r.cacheTtlSeconds(), r.timeoutMs()))
                .toList());
        routes.flush();                    // parents before children
        rules.saveAll(s.rules().stream()
                .map(r -> new RuleEntity(r.routeId(), r.algorithm(), r.limit(), r.windowSeconds()))
                .toList());
        versions.save(new ConfigVersionEntity(s.version() + 1, s.routes().size(), s.rules().size()));
    }
}
