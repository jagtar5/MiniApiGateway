package com.mag.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 100 threads, limit 100, time frozen. 100 threads x 5 attempts = 500 attempts on ONE key.
 * "Leak count" = allowed - limit and MUST be 0: no lost update, no over-admission.
 */
class RateLimitConcurrencyStressTest {

    private static final int THREADS = 100;
    private static final int ATTEMPTS_PER_THREAD = 5;
    private static final int LIMIT = 100;

    private static int hammer(RateLimitAlgorithm algorithm) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                for (int i = 0; i < ATTEMPTS_PER_THREAD; i++) {
                    if (algorithm.tryAcquire("shared-key")) {
                        allowed.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);   // surfaces any exception thrown inside a worker
        }
        pool.shutdownNow();
        return allowed.get();
    }

    @Test
    void tokenBucketHasZeroLeaks() throws Exception {
        int allowed = hammer(new TokenBucketAlgorithm(LIMIT, Duration.ofSeconds(60), new MutableClock(0)));
        assertEquals(0, allowed - LIMIT, "leak count");
    }

    @Test
    void fixedWindowHasZeroLeaks() throws Exception {
        int allowed = hammer(new FixedWindowAlgorithm(LIMIT, Duration.ofSeconds(60), new MutableClock(0)));
        assertEquals(0, allowed - LIMIT, "leak count");
    }

    @Test
    void slidingWindowHasZeroLeaks() throws Exception {
        int allowed = hammer(new SlidingWindowCounterAlgorithm(LIMIT, Duration.ofSeconds(60), new MutableClock(0)));
        assertEquals(0, allowed - LIMIT, "leak count");
    }
}
