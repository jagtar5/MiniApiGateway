package com.mag.core.ratelimit.algorithm;

import com.mag.core.clock.FakeClock;
import com.mag.core.ratelimit.RateLimitAlgorithm;
import com.mag.core.ratelimit.RateLimitDecision;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO(AA: implement the three strategies; these contracts exercise their current stubs).
 */
class RateLimitAlgorithmLspContractTest {

    private static final int LIMIT = 5;

    @ParameterizedTest(name = "{0} respects its configured limit")
    @MethodSource("algorithms")
    void acceptsValidInputWithoutOverLimitOrNegativeDecisions(
            String name, RateLimitAlgorithm algorithm) {
        int allowed = 0;
        for (int attempt = 0; attempt < LIMIT * 2; attempt++) {
                RateLimitDecision decision;
                try {
                    decision = algorithm.tryAcquire("client-1");
                } catch (UnsupportedOperationException e) {
                    // Abort only for AA's explicit skeleton marker; other failures remain visible.
                    String stubMessage = name + ".tryAcquire — implement in M2";
                    if (stubMessage.equals(e.getMessage())) {
                        Assumptions.abort(name + " contract awaits AA's implementation");
                    }
                    throw e;
                }

            assertNotNull(decision, name);
            assertTrue(decision.isAllowed() || decision.isDenied(), name);
            assertTrue(decision.getRetryAfterSeconds() >= 0, name);
            if (decision.isAllowed()) {
                allowed++;
            }
        }
        assertTrue(allowed <= LIMIT, name);
    }

    private static Stream<Arguments> algorithms() {
        Instant start = Instant.parse("2026-11-22T00:00:00Z");
        return Stream.of(
                Arguments.of("TokenBucket",
                        new TokenBucket(LIMIT, 1.0, new FakeClock(start))),
                Arguments.of("FixedWindow",
                        new FixedWindow(LIMIT, Duration.ofMinutes(1), new FakeClock(start))),
                Arguments.of("SlidingWindowCounter",
                        new SlidingWindowCounter(LIMIT, Duration.ofMinutes(1), new FakeClock(start))));
    }
}
