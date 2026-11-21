package com.mag.core.resilience.impl;

import com.mag.core.clock.Clock;
import com.mag.core.resilience.CircuitBreaker;
import com.mag.core.resilience.CircuitState;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * M-D: Resilience Engine — State / CLOSED state.
 *
 * Normal operating state. Counts failures within a time window;
 * transitions to OPEN when failures reach the configured threshold (MAG-QR-02).
 *
 * SRP: knows only the closed-state transition rules.
 */
public class ClosedState implements CircuitState {

    public static final Duration DEFAULT_WINDOW_DURATION = Duration.ofSeconds(60);

    private final FailureWindow failureWindow;

    public ClosedState() {
        this(DEFAULT_WINDOW_DURATION);
    }

    public ClosedState(Duration windowDuration) {
        this.failureWindow = new FailureWindow(windowDuration);
    }

    @Override
    public boolean allowRequest(CircuitBreaker ctx) {
        return true; // CLOSED: all requests are allowed
    }

    @Override
    public void onSuccess(CircuitBreaker ctx) {
        failureWindow.reset();
    }

    @Override
    public void onFailure(CircuitBreaker ctx) {
        int currentFailures = failureWindow.recordFailure(ctx.getClock());
        if (currentFailures >= ctx.getConfig().getFailureThreshold()) {
            ctx.transitionTo(this, new OpenState(ctx.getClock().now()));
        }
    }

    @Override
    public String getName() {
        return "CLOSED";
    }

    public int getFailureCount() {
        return failureWindow.getCount();
    }

    /**
     * Thread-safe counting window for tracking failures within a configured duration.
     */
    public static final class FailureWindow {

        private final Duration windowDuration;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicReference<Instant> windowStart = new AtomicReference<>(null);

        public FailureWindow(Duration windowDuration) {
            this.windowDuration = windowDuration;
        }

        public int recordFailure(Clock clock) {
            Instant now = clock.now();
            Instant start = windowStart.get();

            if (start == null || now.isAfter(start.plus(windowDuration))) {
                if (windowStart.compareAndSet(start, now)) {
                    failureCount.set(1);
                    return 1;
                }
            }
            return failureCount.incrementAndGet();
        }

        public void reset() {
            failureCount.set(0);
            windowStart.set(null);
        }

        public int getCount() {
            return failureCount.get();
        }
    }
}
