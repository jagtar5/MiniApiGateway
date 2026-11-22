package com.mag.gateway.ratelimit;

class SlidingWindowContractTest extends RateLimitAlgorithmContractTest {
    @Override
    RateLimitAlgorithm create(int limit, MutableClock clock) {
        return new SlidingWindowCounterAlgorithm(limit, WINDOW, clock);
    }
}
