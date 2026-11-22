package com.mag.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterFactoryTest {

    private static final Duration W = Duration.ofSeconds(10);

    @Test
    void createsEachBuiltInAlgorithm() {
        RateLimiterFactory f = RateLimiterFactory.withDefaults();
        MutableClock c = new MutableClock(0);
        assertTrue(f.create(new RateLimitConfig("token-bucket", 5, W), c) instanceof TokenBucketAlgorithm);
        assertTrue(f.create(new RateLimitConfig("fixed-window", 5, W), c) instanceof FixedWindowAlgorithm);
        assertTrue(f.create(new RateLimitConfig("sliding-window", 5, W), c) instanceof SlidingWindowCounterAlgorithm);
    }

    @Test
    void nameLookupIsCaseInsensitive() {
        RateLimiterFactory f = RateLimiterFactory.withDefaults();
        assertTrue(f.isRegistered("Token-Bucket"));
    }

    @Test
    void unknownAlgorithmIsRejectedWithClearMessage() {
        RateLimiterFactory f = RateLimiterFactory.withDefaults();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> f.create(new RateLimitConfig("leaky-bucket", 5, W), new MutableClock(0)));
        assertTrue(e.getMessage().contains("leaky-bucket"));
    }

    @Test
    void duplicateRegistrationIsRejected() {
        RateLimiterFactory f = RateLimiterFactory.withDefaults();
        assertThrows(IllegalStateException.class,
                () -> f.register("token-bucket", (cfg, clock) -> new FixedWindowAlgorithm(1, W, clock)));
    }

    /** OCP proof: a 4th algorithm is added using ONLY new code + one register() call. */
    @Test
    void newAlgorithmCanBeAddedWithoutEditingExistingClasses() {
        RateLimiterFactory f = RateLimiterFactory.withDefaults();
        assertFalse(f.isRegistered("no-op"));

        // brand-new algorithm, defined here in the test, touching no production file
        RateLimitAlgorithm alwaysAllow = key -> true;
        f.register("no-op", (cfg, clock) -> alwaysAllow);

        RateLimitAlgorithm made = f.create(new RateLimitConfig("no-op", 1, W), new MutableClock(0));
        assertTrue(made.tryAcquire("k"));
        assertTrue(made.tryAcquire("k"));
        assertEquals(4, f.algorithmNames().size());
    }
}
