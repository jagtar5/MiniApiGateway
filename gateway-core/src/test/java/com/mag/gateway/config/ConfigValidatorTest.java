package com.mag.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigValidatorTest {

    private final ConfigValidator validator =
            new ConfigValidator(() -> Set.of("token-bucket", "fixed-window", "sliding-window"));

    static RouteConfig route(String id) {
        return new RouteConfig(id, "/" + id + "/**", "http://localhost:9000/" + id, 30, 2000);
    }

    private static ConfigSnapshot snap(List<RouteConfig> routes, List<RateLimitRuleConfig> rules) {
        return new ConfigSnapshot(0, routes, rules);
    }

    private static boolean hasError(List<FieldError> errors, String field, String reasonPart) {
        return errors.stream().anyMatch(e -> e.field().equals(field) && e.reason().contains(reasonPart));
    }

    @Test
    void validConfigHasNoErrors() {
        ConfigSnapshot s = snap(List.of(route("fast")),
                List.of(new RateLimitRuleConfig("fast", "token-bucket", 10, 60)));
        assertTrue(validator.validate(s).isEmpty());
    }

    @Test
    void rejectsNegativeTtl() {
        RouteConfig bad = new RouteConfig("r", "/r", "http://h/", -1, 1000);
        assertTrue(hasError(validator.validate(snap(List.of(bad), List.of())), "routes[0].cacheTtlSeconds", ">= 0"));
    }

    @Test
    void zeroTtlIsAllowedAndMeansNoCaching() {
        RouteConfig ok = new RouteConfig("r", "/r", "http://h/", 0, 1000);
        assertTrue(validator.validate(snap(List.of(ok), List.of())).isEmpty());
    }

    @Test
    void rejectsMissingUpstreamUrl() {
        RouteConfig bad = new RouteConfig("r", "/r", null, 0, 1000);
        assertTrue(hasError(validator.validate(snap(List.of(bad), List.of())), "routes[0].upstreamUrl", "blank"));
    }

    @Test
    void rejectsNonHttpUpstreamUrl() {
        RouteConfig bad = new RouteConfig("r", "/r", "ftp://h/", 0, 1000);
        assertTrue(hasError(validator.validate(snap(List.of(bad), List.of())), "routes[0].upstreamUrl", "http"));
    }

    @Test
    void rejectsMissingPathPattern() {
        RouteConfig bad = new RouteConfig("r", "  ", "http://h/", 0, 1000);
        assertTrue(hasError(validator.validate(snap(List.of(bad), List.of())), "routes[0].pathPattern", "blank"));
    }

    @Test
    void rejectsDuplicateRouteIds() {
        List<FieldError> e = validator.validate(snap(List.of(route("a"), route("a")), List.of()));
        assertTrue(hasError(e, "routes[1].id", "duplicate"));
    }

    @Test
    void rejectsRuleForUnknownRouteAndBadNumbers() {
        List<FieldError> e = validator.validate(snap(List.of(route("a")),
                List.of(new RateLimitRuleConfig("ghost", "token-bucket", 0, -5))));
        assertTrue(hasError(e, "rules[0].routeId", "unknown route"));
        assertTrue(hasError(e, "rules[0].limit", "> 0"));
        assertTrue(hasError(e, "rules[0].windowSeconds", "> 0"));
    }

    @Test
    void rejectsUnknownAlgorithm() {
        List<FieldError> e = validator.validate(snap(List.of(route("a")),
                List.of(new RateLimitRuleConfig("a", "magic", 5, 60))));
        assertTrue(hasError(e, "rules[0].algorithm", "unknown algorithm"));
    }

    @Test
    void reportsAllProblemsAtOnce() {
        RouteConfig bad = new RouteConfig(null, null, null, -1, 0);
        assertEquals(5, validator.validate(snap(List.of(bad), List.of())).size());
    }

    @Test
    void validateOrThrowCarriesTheFieldErrors() {
        RouteConfig bad = new RouteConfig("r", "/r", "http://h/", -1, 1000);
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validateOrThrow(snap(List.of(bad), List.of())));
        assertEquals("routes[0].cacheTtlSeconds", ex.errors().get(0).field());
    }
}
