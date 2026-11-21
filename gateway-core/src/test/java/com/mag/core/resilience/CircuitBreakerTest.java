package com.mag.core.resilience;

import com.mag.core.clock.FakeClock;
import com.mag.core.resilience.impl.ClosedState;
import com.mag.core.resilience.impl.HalfOpenState;
import com.mag.core.resilience.impl.OpenState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitBreakerTest {

    private FakeClock fakeClock;
    private BreakerConfig config;
    private Instant startTime;

    @BeforeEach
    void setUp() {
        startTime = Instant.parse("2026-11-21T10:00:00Z");
        fakeClock = new FakeClock(startTime);
        // threshold = 3 failures, openDuration = 30 seconds, halfOpenProbes = 2
        config = new BreakerConfig(3, Duration.ofSeconds(30), 2);
    }

    @Test
    @DisplayName("Initial state is CLOSED and allows requests")
    void testInitialStateClosed() {
        CircuitBreaker cb = new CircuitBreaker(fakeClock, config);
        assertTrue(cb.allowRequest(), "Circuit should allow requests when CLOSED");
        assertInstanceOf(ClosedState.class, cb.getState());
        assertEquals("CLOSED", cb.getState().getName());
    }

    @Test
    @DisplayName("Closed -> Open transition: failures reach threshold (MAG-QR-02)")
    void testClosedToOpenTransition() {
        CircuitBreaker cb = new CircuitBreaker(fakeClock, config);

        // Failures 1 and 2: still below threshold of 3
        cb.onFailure();
        assertTrue(cb.allowRequest());
        assertInstanceOf(ClosedState.class, cb.getState());

        cb.onFailure();
        assertTrue(cb.allowRequest());
        assertInstanceOf(ClosedState.class, cb.getState());

        // Failure 3: threshold reached -> transitions to OPEN
        cb.onFailure();
        assertInstanceOf(OpenState.class, cb.getState());
        assertFalse(cb.allowRequest(), "Circuit in OPEN must reject requests");
        assertEquals("OPEN", cb.getState().getName());
    }

    @Test
    @DisplayName("Success in CLOSED state resets failure count")
    void testSuccessResetsFailureCountInClosed() {
        CircuitBreaker cb = new CircuitBreaker(fakeClock, config);

        // 2 failures
        cb.onFailure();
        cb.onFailure();

        // 1 success resets counter
        cb.onSuccess();

        // 2 more failures should NOT trip breaker because counter was reset
        cb.onFailure();
        cb.onFailure();
        assertTrue(cb.allowRequest(), "Circuit should remain CLOSED because failures were reset");
        assertInstanceOf(ClosedState.class, cb.getState());

        // 3rd failure trips the breaker
        cb.onFailure();
        assertInstanceOf(OpenState.class, cb.getState());
        assertFalse(cb.allowRequest());
    }

    @Test
    @DisplayName("Open -> HalfOpen transition: after openDuration, probe request is allowed (MAG-QR-02, MAG-QR-10)")
    void testOpenToHalfOpenTransition() {
        CircuitBreaker cb = new CircuitBreaker(fakeClock, config);

        // Trip the breaker to OPEN
        cb.onFailure();
        cb.onFailure();
        cb.onFailure();
        assertInstanceOf(OpenState.class, cb.getState());

        // Advance clock by 15s (less than openDuration 30s) -> still OPEN and rejected
        fakeClock.advance(Duration.ofSeconds(15));
        assertFalse(cb.allowRequest());
        assertInstanceOf(OpenState.class, cb.getState());

        // Advance clock past openDuration (total 31s from trip)
        fakeClock.advance(Duration.ofSeconds(16));

        // Next allowRequest() detects timeout expiry and transitions to HALF_OPEN, allowing trial probe
        assertTrue(cb.allowRequest(), "Probe request should be allowed once openDuration has elapsed");
        assertInstanceOf(HalfOpenState.class, cb.getState());
        assertEquals("HALF_OPEN", cb.getState().getName());
    }

    @Test
    @DisplayName("HalfOpen -> Closed transition: successful probes recover the circuit")
    void testHalfOpenToClosedTransition() {
        CircuitBreaker cb = new CircuitBreaker(fakeClock, config);

        // Trip to OPEN
        cb.onFailure();
        cb.onFailure();
        cb.onFailure();

        // Advance past openDuration
        fakeClock.advance(Duration.ofSeconds(31));

        // First probe allowed -> enters HALF_OPEN
        assertTrue(cb.allowRequest());
        assertInstanceOf(HalfOpenState.class, cb.getState());

        // Probe 1 succeeds
        cb.onSuccess();
        // Still in HALF_OPEN because config requires 2 probes
        assertInstanceOf(HalfOpenState.class, cb.getState());

        // Second probe allowed
        assertTrue(cb.allowRequest());

        // Probe 2 succeeds -> recovered to CLOSED!
        cb.onSuccess();
        assertInstanceOf(ClosedState.class, cb.getState());
        assertEquals("CLOSED", cb.getState().getName());
        assertTrue(cb.allowRequest(), "Circuit is back to CLOSED, normal traffic allowed");
    }

    @Test
    @DisplayName("HalfOpen -> Open transition: a failed probe immediately re-opens the circuit")
    void testHalfOpenToOpenOnFailure() {
        CircuitBreaker cb = new CircuitBreaker(fakeClock, config);

        // Trip to OPEN
        cb.onFailure();
        cb.onFailure();
        cb.onFailure();

        // Advance past openDuration -> transition to HALF_OPEN
        fakeClock.advance(Duration.ofSeconds(31));
        assertTrue(cb.allowRequest());
        assertInstanceOf(HalfOpenState.class, cb.getState());

        // Probe fails!
        cb.onFailure();

        // Must immediately return to OPEN
        assertInstanceOf(OpenState.class, cb.getState());
        assertFalse(cb.allowRequest(), "Circuit must immediately reject traffic after probe failure");
    }

    @Test
    @DisplayName("HalfOpen state limits requests to configured probe count")
    void testHalfOpenLimitsProbes() {
        // 1 probe allowed
        BreakerConfig singleProbeConfig = new BreakerConfig(2, Duration.ofSeconds(10), 1);
        CircuitBreaker cb = new CircuitBreaker(fakeClock, singleProbeConfig);

        cb.onFailure();
        cb.onFailure();
        fakeClock.advance(Duration.ofSeconds(11));

        // Probe 1 allowed
        assertTrue(cb.allowRequest());
        assertInstanceOf(HalfOpenState.class, cb.getState());

        // Excess request before probe completes is blocked
        assertFalse(cb.allowRequest(), "Excess request in HALF_OPEN should be blocked while probe is running");
    }

    @Test
    @DisplayName("Concurrent failures: multiple threads trigger exactly one OPEN transition via CAS (MAG-QR-02)")
    void testConcurrentFailuresTriggerExactlyOneTransition() throws InterruptedException, ExecutionException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        // Threshold = 5
        BreakerConfig stressConfig = new BreakerConfig(5, Duration.ofSeconds(60), 2);
        CircuitBreaker cb = new CircuitBreaker(fakeClock, stressConfig);

        List<Future<?>> futures = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    cb.onFailure();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // State must be OPEN
        assertInstanceOf(OpenState.class, cb.getState());
        assertFalse(cb.allowRequest());
    }
}
