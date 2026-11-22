package com.mag.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RateLimitGuardTest {

    private static RateLimitGuard guardWithLimit(int limit) {
        RateLimiterRegistry reg = new RateLimiterRegistry(RateLimiterFactory.withDefaults(), new MutableClock(0));
        reg.replaceAll(Map.of("r1", new RateLimitConfig("fixed-window", limit, Duration.ofSeconds(60))));
        return new RateLimitGuard(r -> r.header("X-User") == null ? null : "u:" + r.header("X-User"),
                new ClientIpKeyExtractor(), reg);
    }

    private static RequestView user(String u) {
        return new RequestView(Map.of("X-User", u), "9.9.9.9");
    }

    @Test
    void routeWithoutRuleIsNotLimited() {
        RateLimitGuard.Result r = guardWithLimit(1).check(user("a"), "other-route");
        assertTrue(r.allowed());
        assertFalse(r.ruleApplied());
    }

    @Test
    void shortCircuitsAfterLimit() {
        RateLimitGuard g = guardWithLimit(2);
        assertTrue(g.check(user("a"), "r1").allowed());
        assertTrue(g.check(user("a"), "r1").allowed());
        assertFalse(g.check(user("a"), "r1").allowed());     // N+1 -> 429
        assertTrue(g.check(user("b"), "r1").allowed());      // other user unaffected
    }

    @Test
    void fallsBackToClientIpWhenPrimaryKeyMissing() {
        RateLimitGuard g = guardWithLimit(1);
        RequestView anon = new RequestView(Map.of(), "7.7.7.7");
        assertEquals("ip:7.7.7.7", g.check(anon, "r1").key());
    }

    @Test
    void unidentifiableCallersShareOneAnonymousBucket() {
        RateLimitGuard g = guardWithLimit(1);
        RequestView nobody = new RequestView(Map.of(), null);
        assertTrue(g.check(nobody, "r1").allowed());
        assertFalse(g.check(nobody, "r1").allowed());
    }

    @Test
    void unchangedRuleKeepsItsCountersAcrossReload() {
        MutableClock clock = new MutableClock(0);
        RateLimiterRegistry reg = new RateLimiterRegistry(RateLimiterFactory.withDefaults(), clock);
        RateLimitConfig cfg = new RateLimitConfig("fixed-window", 1, Duration.ofSeconds(60));
        reg.replaceAll(Map.of("r1", cfg));
        assertTrue(reg.forRoute("r1").get().tryAcquire("k"));
        reg.replaceAll(Map.of("r1", cfg));                  // same rule re-applied
        assertFalse(reg.forRoute("r1").get().tryAcquire("k"));
    }
}
