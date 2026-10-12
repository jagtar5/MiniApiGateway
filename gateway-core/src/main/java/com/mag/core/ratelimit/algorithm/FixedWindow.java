package com.mag.core.ratelimit.algorithm;

import com.mag.core.clock.Clock;
import com.mag.core.ratelimit.RateLimitAlgorithm;
import com.mag.core.ratelimit.RateLimitDecision;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M-B: Identity &amp; Traffic Control — Strategy / Fixed Window algorithm.
 *
 * Counts requests in discrete time windows.
 * Window boundaries are computed from the injected {@link Clock} (no sleeps).
 * Atomic counter updates via {@link ConcurrentHashMap#compute} (MAG-QR-01).
 */
public class FixedWindow implements RateLimitAlgorithm {

    private final int limit;
    private final Duration window;
    private final Clock clock;

    // TODO M2: define inner Window state class and ConcurrentHashMap<String, Window>

    public FixedWindow(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = Objects.requireNonNull(window);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public RateLimitDecision tryAcquire(String key) {
        // TODO M2: atomically check and increment window counter; reset when window expires.
        throw new UnsupportedOperationException("FixedWindow.tryAcquire — implement in M2");
    }
}
