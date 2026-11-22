package com.mag.gateway.config;

import java.util.List;
import java.util.Optional;

/**
 * Immutable view of the whole gateway configuration. A record + {@code List.copyOf} means no
 * field can be reassigned and no list can be mutated after construction, so readers on the
 * request path can use a snapshot without any lock.
 *
 * <p>If Ahmad's P0-1 PR already put a ConfigSnapshot in gateway-core, keep that one and adapt
 * the field list; the rest of this package only needs routes(), rules() and version().
 */
public record ConfigSnapshot(long version, List<RouteConfig> routes, List<RateLimitRuleConfig> rules) {

    public static final ConfigSnapshot EMPTY = new ConfigSnapshot(0, List.of(), List.of());

    public ConfigSnapshot {
        routes = routes == null ? List.of() : List.copyOf(routes);
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public Optional<RouteConfig> route(String id) {
        return routes.stream().filter(r -> java.util.Objects.equals(r.id(), id)).findFirst();
    }

    public Optional<RateLimitRuleConfig> rule(String routeId) {
        return rules.stream().filter(r -> java.util.Objects.equals(r.routeId(), routeId)).findFirst();
    }
}
