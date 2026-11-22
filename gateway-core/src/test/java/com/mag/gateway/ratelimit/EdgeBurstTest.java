package com.mag.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Shows WHY three algorithms exist: a burst straddling a window boundary.
 * Limit 10 per 10 s. 10 requests at t=9.999 s and 10 more at t=10.000 s (1 ms apart).
 */
class EdgeBurstTest {

    private static final Duration WINDOW = Duration.ofSeconds(10);

    private static int burst(RateLimitAlgorithm a, MutableClock clock) {
        int allowed = 0;
        clock.set(9_999);
        for (int i = 0; i < 10; i++) {
            if (a.tryAcquire("k")) {
                allowed++;
            }
        }
        clock.set(10_000);
        for (int i = 0; i < 10; i++) {
            if (a.tryAcquire("k")) {
                allowed++;
            }
        }
        return allowed;
    }

    @Test
    void fixedWindowAllowsDoubleBurstAtBoundary() {
        MutableClock clock = new MutableClock(0);
        assertEquals(20, burst(new FixedWindowAlgorithm(10, WINDOW, clock), clock));
    }

    @Test
    void tokenBucketAbsorbsTheBoundaryBurst() {
        MutableClock clock = new MutableClock(0);
        assertEquals(10, burst(new TokenBucketAlgorithm(10, WINDOW, clock), clock));
    }

    @Test
    void slidingWindowAbsorbsTheBoundaryBurst() {
        MutableClock clock = new MutableClock(0);
        assertEquals(10, burst(new SlidingWindowCounterAlgorithm(10, WINDOW, clock), clock));
    }
}
