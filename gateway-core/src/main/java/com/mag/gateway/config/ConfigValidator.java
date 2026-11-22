package com.mag.gateway.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Validates a candidate snapshot BEFORE it may replace the live one. Pure function of its
 * input: no I/O, no shared state, therefore trivially thread-safe.
 */
public class ConfigValidator {

    private final Supplier<Set<String>> knownAlgorithms;

    /** @param knownAlgorithms supplier so algorithms registered later (OCP) are still accepted */
    public ConfigValidator(Supplier<Set<String>> knownAlgorithms) {
        this.knownAlgorithms = java.util.Objects.requireNonNull(knownAlgorithms, "knownAlgorithms");
    }

    public List<FieldError> validate(ConfigSnapshot snapshot) {
        List<FieldError> errors = new ArrayList<>();
        Set<String> routeIds = new HashSet<>();

        List<RouteConfig> routes = snapshot.routes();
        for (int i = 0; i < routes.size(); i++) {
            RouteConfig r = routes.get(i);
            String p = "routes[" + i + "].";
            if (isBlank(r.id())) {
                errors.add(new FieldError(p + "id", "must not be blank"));
            } else if (!routeIds.add(r.id())) {
                errors.add(new FieldError(p + "id", "duplicate route id '" + r.id() + "'"));
            }
            if (isBlank(r.pathPattern())) {
                errors.add(new FieldError(p + "pathPattern", "must not be blank"));
            } else if (!r.pathPattern().startsWith("/")) {
                errors.add(new FieldError(p + "pathPattern", "must start with '/'"));
            }
            if (isBlank(r.upstreamUrl())) {
                errors.add(new FieldError(p + "upstreamUrl", "must not be blank"));
            } else if (!isHttpUrl(r.upstreamUrl())) {
                errors.add(new FieldError(p + "upstreamUrl", "must be an absolute http(s) URL"));
            }
            if (r.cacheTtlSeconds() < 0) {
                errors.add(new FieldError(p + "cacheTtlSeconds", "must be >= 0 (0 disables caching)"));
            }
            if (r.timeoutMs() <= 0) {
                errors.add(new FieldError(p + "timeoutMs", "must be > 0"));
            }
        }

        Set<String> known = lower(knownAlgorithms.get());
        Set<String> ruledRoutes = new HashSet<>();
        List<RateLimitRuleConfig> rules = snapshot.rules();
        for (int i = 0; i < rules.size(); i++) {
            RateLimitRuleConfig r = rules.get(i);
            String p = "rules[" + i + "].";
            if (isBlank(r.routeId())) {
                errors.add(new FieldError(p + "routeId", "must not be blank"));
            } else {
                if (!routeIds.contains(r.routeId())) {
                    errors.add(new FieldError(p + "routeId", "references unknown route '" + r.routeId() + "'"));
                }
                if (!ruledRoutes.add(r.routeId())) {
                    errors.add(new FieldError(p + "routeId", "route '" + r.routeId() + "' already has a rule"));
                }
            }
            if (isBlank(r.algorithm())) {
                errors.add(new FieldError(p + "algorithm", "must not be blank"));
            } else if (!known.contains(r.algorithm().trim().toLowerCase(Locale.ROOT))) {
                errors.add(new FieldError(p + "algorithm",
                        "unknown algorithm '" + r.algorithm() + "', expected one of " + known));
            }
            if (r.limit() <= 0) {
                errors.add(new FieldError(p + "limit", "must be > 0"));
            }
            if (r.windowSeconds() <= 0) {
                errors.add(new FieldError(p + "windowSeconds", "must be > 0"));
            }
        }
        return List.copyOf(errors);
    }

    public void validateOrThrow(ConfigSnapshot snapshot) {
        List<FieldError> errors = validate(snapshot);
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isHttpUrl(String s) {
        try {
            URI u = new URI(s.trim());
            String scheme = u.getScheme();
            return u.getHost() != null && scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static Set<String> lower(Set<String> in) {
        Set<String> out = new java.util.TreeSet<>();
        for (String s : in) {
            out.add(s.toLowerCase(Locale.ROOT));
        }
        return out;
    }
}
