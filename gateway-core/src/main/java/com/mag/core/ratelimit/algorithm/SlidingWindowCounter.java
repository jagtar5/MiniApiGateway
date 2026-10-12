package com.mag.core.ratelimit.algorithm;

import com.mag.core.clock.Clock;
import com.mag.core.ratelimit.RateLimitAlgorithm;
import com.mag.core.ratelimit.RateLimitDecision;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M-B: Identity &amp; Traffic Control — Strategy / Sliding Window Counter algorithm.
 *
 * Smooths the fixed-window boundary burst by weighting the previous window's count.
 * Atomic counter updates via {@link ConcurrentHashMap#compute} (MAG-QR-01).
 * Time from injected {@link Clock}.
 */
public class SlidingWindowCounter implements RateLimitAlgorithm {

    private final int limit;
    private final Duration window;
    private final Clock clock;

    // TODO M2: define inner Counts state class and ConcurrentHashMap<String, Counts>

    public SlidingWindowCounter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = Objects.requireNonNull(window);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public RateLimitDecision tryAcquire(String key) {
        // TODO M2: compute weighted count from previous and current windows; deny if over limit.
        throw new UnsupportedOperationException("SlidingWindowCounter.tryAcquire — implement in M2");
    }
}
