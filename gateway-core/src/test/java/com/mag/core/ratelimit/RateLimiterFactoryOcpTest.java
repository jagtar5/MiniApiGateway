package com.mag.core.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterFactoryOcpTest {

    @Test
    void registersANewAlgorithmWithoutChangingExistingAlgorithms() {
        RateLimiterFactory factory = new RateLimiterFactory();
        RateLimitAlgorithm noOpAlgorithm = new NoOpRateLimitAlgorithm();

        factory.register("NO_OP", ignored -> noOpAlgorithm);

        assertSame(noOpAlgorithm, factory.create("NO_OP", Map.of()));
        assertTrue(factory.create("no_op", Map.of()).tryAcquire("client-1").isAllowed());
    }

    private static final class NoOpRateLimitAlgorithm implements RateLimitAlgorithm {
        @Override
        public RateLimitDecision tryAcquire(String key) {
            return RateLimitDecision.allowed();
        }
    }
}
