package com.mag.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * LSP/contract test: every RateLimitAlgorithm must pass these. Each concrete algorithm has a tiny
 * subclass that only says how to build itself, so a new algorithm gets the whole suite for free.
 */
abstract class RateLimitAlgorithmContractTest {

    protected static final Duration WINDOW = Duration.ofSeconds(10);

    abstract RateLimitAlgorithm create(int limit, MutableClock clock);

    @Test
    void neverExceedsLimitWithinOneWindow() {
        MutableClock clock = new MutableClock(0);
        RateLimitAlgorithm a = create(5, clock);
        int allowed = 0;
        for (int i = 0; i < 50; i++) {
            if (a.tryAcquire("k")) {
                allowed++;
            }
        }
        assertEquals(5, allowed);
    }

    @Test
    void doesNotThrowForValidKeys() {
        RateLimitAlgorithm a = create(3, new MutableClock(0));
        a.tryAcquire("a");
        a.tryAcquire("user:42");
        a.tryAcquire("ip:10.0.0.1");
    }

    @Test
    void rejectsNullAndBlankKeys() {
        RateLimitAlgorithm a = create(3, new MutableClock(0));
        assertThrows(IllegalArgumentException.class, () -> a.tryAcquire(null));
        assertThrows(IllegalArgumentException.class, () -> a.tryAcquire(""));
        assertThrows(IllegalArgumentException.class, () -> a.tryAcquire("   "));
    }

    @Test
    void keysAreIndependent() {
        RateLimitAlgorithm a = create(1, new MutableClock(0));
        assertTrue(a.tryAcquire("a"));
        assertFalse(a.tryAcquire("a"));
        assertTrue(a.tryAcquire("b"));
    }

    @Test
    void allowsAgainAfterEnoughTimePasses() {
        MutableClock clock = new MutableClock(0);
        RateLimitAlgorithm a = create(2, clock);
        assertTrue(a.tryAcquire("k"));
        assertTrue(a.tryAcquire("k"));
        assertFalse(a.tryAcquire("k"));
        clock.advance(2 * WINDOW.toMillis());
        assertTrue(a.tryAcquire("k"));
    }

    @Test
    void boundaryNMinusOneNAndNPlusOne() {
        int n = 10;
        MutableClock clock = new MutableClock(0);
        RateLimitAlgorithm a = create(n, clock);
        for (int i = 1; i <= n - 1; i++) {
            assertTrue(a.tryAcquire("k"), "request " + i + " (below N) must pass");
        }
        assertTrue(a.tryAcquire("k"), "request N must pass");
        assertFalse(a.tryAcquire("k"), "request N+1 must be rejected");
    }

    @Test
    void firstRequestOfAnUnseenKeyIsAllowed() {
        RateLimitAlgorithm a = create(10, new MutableClock(0));
        assertTrue(a.tryAcquire("never-seen-before"));
    }

    @Test
    void oneRequestBelowTheLimitIsAllowedAndLeavesRoom() {
        RateLimitAlgorithm a = create(10, new MutableClock(0));
        assertTrue(a.tryAcquire("k"));
        for (int i = 0; i < 8; i++) {
            assertTrue(a.tryAcquire("k"));
        }
        assertTrue(a.tryAcquire("k"), "10th request still inside the limit");
    }

    @Test
    void limitOfOneAllowsExactlyOne() {
        RateLimitAlgorithm a = create(1, new MutableClock(0));
        assertTrue(a.tryAcquire("k"));
        assertFalse(a.tryAcquire("k"));
    }

    @Test
    void zeroLimitIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> create(0, new MutableClock(0)));
        assertThrows(IllegalArgumentException.class, () -> create(-1, new MutableClock(0)));
    }
}
