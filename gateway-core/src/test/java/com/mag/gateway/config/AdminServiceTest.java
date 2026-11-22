package com.mag.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminServiceTest {

    private final InMemoryConfigStore store = new InMemoryConfigStore();
    private final ConfigManager manager =
            new ConfigManager(new ConfigValidator(() -> Set.of("token-bucket", "fixed-window", "sliding-window")));
    private final AdminService service = new AdminService(manager, store);

    private static RouteConfig route(String id) {
        return ConfigValidatorTest.route(id);
    }

    @Test
    void createRoutePersistsAndGoesLive() {
        service.createRoute(route("fast"));
        assertEquals(1, manager.current().routes().size());
        assertEquals(1, store.load().routes().size());
    }

    @Test
    void createRuleForExistingRoute() {
        service.createRoute(route("fast"));
        service.createRule(new RateLimitRuleConfig("fast", "token-bucket", 100, 60));
        assertEquals(100, manager.current().rule("fast").get().limit());
    }

    @Test
    void updateRuleLimitIsVisibleImmediately() {
        service.createRoute(route("fast"));
        service.createRule(new RateLimitRuleConfig("fast", "token-bucket", 100, 60));
        service.updateRule("fast", new RateLimitRuleConfig("fast", "token-bucket", 5, 60));
        assertEquals(5, manager.current().rule("fast").get().limit());
    }

    @Test
    void badPayloadErrorNamesTheFieldAndTheReason() {
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> service.createRoute(new RouteConfig("r", "/r", "http://h/", -10, 1000)));
        FieldError first = ex.errors().get(0);
        assertEquals("routes[0].cacheTtlSeconds", first.field());
        assertTrue(first.reason().contains(">= 0"));
    }

    @Test
    void negativeLimitIsRejectedWithFieldName() {
        service.createRoute(route("fast"));
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> service.createRule(new RateLimitRuleConfig("fast", "token-bucket", -1, 60)));
        assertEquals("rules[0].limit", ex.errors().get(0).field());
    }

    @Test
    void missingRequiredFieldIsRejected() {
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> service.createRoute(new RouteConfig("r", null, "http://h/", 0, 1000)));
        assertEquals("routes[0].pathPattern", ex.errors().get(0).field());
    }

    @Test
    void nullBodyIsRejected() {
        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> service.createRoute(null));
        assertEquals("body", ex.errors().get(0).field());
    }

    @Test
    void invalidChangeLeavesLiveAndStoredConfigUntouched() {
        service.createRoute(route("fast"));
        ConfigSnapshot liveBefore = manager.current();
        ConfigSnapshot storedBefore = store.load();
        assertThrows(ConfigValidationException.class,
                () -> service.updateRoute("fast", new RouteConfig("fast", "/f", "http://h/", -1, 1000)));
        assertSame(liveBefore, manager.current());
        assertSame(storedBefore, store.load());
    }

    @Test
    void databaseFailureLeavesLiveConfigUntouched() {
        service.createRoute(route("fast"));
        ConfigSnapshot liveBefore = manager.current();
        store.failOnSave(true);
        assertThrows(IllegalStateException.class, () -> service.createRoute(route("slow")));
        assertSame(liveBefore, manager.current());
    }

    @Test
    void unknownRouteIsNotFound() {
        assertThrows(ConfigNotFoundException.class, () -> service.updateRoute("nope", route("nope")));
        assertThrows(ConfigNotFoundException.class, () -> service.deleteRoute("nope"));
        assertThrows(ConfigNotFoundException.class, () -> service.deleteRule("nope"));
    }

    @Test
    void pathIdMustMatchBodyId() {
        service.createRoute(route("fast"));
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> service.updateRoute("fast", route("other")));
        assertEquals("id", ex.errors().get(0).field());
    }

    @Test
    void deletingARouteThatStillHasARuleIsRejected() {
        service.createRoute(route("fast"));
        service.createRule(new RateLimitRuleConfig("fast", "token-bucket", 10, 60));
        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> service.deleteRoute("fast"));
        assertEquals("rules[0].routeId", ex.errors().get(0).field());
        service.deleteRule("fast");
        service.deleteRoute("fast");
        assertEquals(0, manager.current().routes().size());
    }

    @Test
    void duplicateRouteIsRejected() {
        service.createRoute(route("fast"));
        assertThrows(ConfigValidationException.class, () -> service.createRoute(route("fast")));
    }
}
