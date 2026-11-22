package com.mag.gateway.ratelimit;

class TokenBucketContractTest extends RateLimitAlgorithmContractTest {
    @Override
    RateLimitAlgorithm create(int limit, MutableClock clock) {
        return new TokenBucketAlgorithm(limit, WINDOW, clock);
    }
}
