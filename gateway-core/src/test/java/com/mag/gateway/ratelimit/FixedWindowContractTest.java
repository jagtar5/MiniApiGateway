package com.mag.gateway.ratelimit;

class FixedWindowContractTest extends RateLimitAlgorithmContractTest {
    @Override
    RateLimitAlgorithm create(int limit, MutableClock clock) {
        return new FixedWindowAlgorithm(limit, WINDOW, clock);
    }
}
