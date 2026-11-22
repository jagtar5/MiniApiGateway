package com.mag.gateway.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic behind the Admin REST API (framework-free so it is unit-testable).
 * Every mutation: build candidate -> validate -> persist -> swap live snapshot.
 * If validation or persistence fails, the live snapshot and the database are unchanged.
 */
public class AdminService {

    private final ConfigManager manager;
    private final ConfigStore store;

    public AdminService(ConfigManager manager, ConfigStore store) {
        this.manager = manager;
        this.store = store;
    }

    public List<RouteConfig> listRoutes() {
        return manager.current().routes();
    }

    public List<RateLimitRuleConfig> listRules() {
        return manager.current().rules();
    }

    public synchronized RouteConfig createRoute(RouteConfig route) {
        ConfigSnapshot base = manager.current();
        List<RouteConfig> routes = new ArrayList<>(base.routes());
        routes.add(requireBody(route));
        apply(new ConfigSnapshot(base.version(), routes, base.rules()));
        return route;
    }

    public synchronized RouteConfig updateRoute(String id, RouteConfig route) {
        requireBody(route);
        ConfigSnapshot base = manager.current();
        if (route.id() != null && !route.id().equals(id)) {
            throw new ConfigValidationException(List.of(new FieldError("id", "body id must match path id '" + id + "'")));
        }
        List<RouteConfig> routes = new ArrayList<>(base.routes());
        int idx = indexOfRoute(routes, id);
        routes.set(idx, route);
        apply(new ConfigSnapshot(base.version(), routes, base.rules()));
        return route;
    }

    public synchronized void deleteRoute(String id) {
        ConfigSnapshot base = manager.current();
        List<RouteConfig> routes = new ArrayList<>(base.routes());
        routes.remove(indexOfRoute(routes, id));
        // rules of the deleted route are left in place on purpose: validation then rejects the
        // delete (rules[i].routeId references unknown route), so nothing is deleted silently.
        apply(new ConfigSnapshot(base.version(), routes, base.rules()));
    }

    public synchronized RateLimitRuleConfig createRule(RateLimitRuleConfig rule) {
        ConfigSnapshot base = manager.current();
        List<RateLimitRuleConfig> rules = new ArrayList<>(base.rules());
        rules.add(requireBody(rule));
        apply(new ConfigSnapshot(base.version(), base.routes(), rules));
        return rule;
    }

    public synchronized RateLimitRuleConfig updateRule(String routeId, RateLimitRuleConfig rule) {
        requireBody(rule);
        ConfigSnapshot base = manager.current();
        if (rule.routeId() != null && !rule.routeId().equals(routeId)) {
            throw new ConfigValidationException(
                    List.of(new FieldError("routeId", "body routeId must match path routeId '" + routeId + "'")));
        }
        List<RateLimitRuleConfig> rules = new ArrayList<>(base.rules());
        int idx = indexOfRule(rules, routeId);
        rules.set(idx, rule);
        apply(new ConfigSnapshot(base.version(), base.routes(), rules));
        return rule;
    }

    public synchronized void deleteRule(String routeId) {
        ConfigSnapshot base = manager.current();
        List<RateLimitRuleConfig> rules = new ArrayList<>(base.rules());
        rules.remove(indexOfRule(rules, routeId));
        apply(new ConfigSnapshot(base.version(), base.routes(), rules));
    }

    /** Re-reads the database and applies it (manual hot reload). */
    public synchronized ConfigSnapshot reload() {
        return manager.reloadFrom(store);
    }

    private void apply(ConfigSnapshot candidate) {
        manager.validate(candidate);   // 1. reject bad input before touching the DB
        store.save(candidate);         // 2. persist (throws -> live config untouched)
        manager.reload(candidate);     // 3. atomic swap
    }

    private static <T> T requireBody(T body) {
        if (body == null) {
            throw new ConfigValidationException(List.of(new FieldError("body", "request body is required")));
        }
        return body;
    }

    private static int indexOfRoute(List<RouteConfig> routes, String id) {
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new ConfigNotFoundException("route not found: " + id);
    }

    private static int indexOfRule(List<RateLimitRuleConfig> rules, String routeId) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).routeId().equals(routeId)) {
                return i;
            }
        }
        throw new ConfigNotFoundException("rule not found for route: " + routeId);
    }
}
