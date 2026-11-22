package com.mag.gateway.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window counter: weighs the previous window by how much of it still overlaps the
 * sliding window: {@code estimate = previous * (1 - elapsedFraction) + current}.
 * Smooths the boundary burst of the fixed window using only two counters per key.
 */
public final class SlidingWindowCounterAlgorithm implements RateLimitAlgorithm {

    private record Counters(long windowId, int current, int previous) { }

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Counters> state = new ConcurrentHashMap<>();

    public SlidingWindowCounterAlgorithm(int limit, Duration window, Clock clock) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        this.limit = limit;
        this.windowMillis = Keys.windowMillis(window);
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @Override
    public boolean tryAcquire(String key) {
        Keys.require(key);
        final long now = clock.millis();
        final long id = Math.floorDiv(now, windowMillis);
        final double elapsedFraction = (double) Math.floorMod(now, windowMillis) / windowMillis;
        final boolean[] allowed = new boolean[1];
        state.compute(key, (k, s) -> {
            int current;
            int previous;
            if (s == null) {
                current = 0;
                previous = 0;
            } else if (s.windowId() == id) {
                current = s.current();
                previous = s.previous();
            } else if (s.windowId() == id - 1) {
                current = 0;
                previous = s.current();
            } else {
                current = 0;
                previous = 0;
            }
            double estimate = previous * (1.0 - elapsedFraction) + current;
            if (estimate + 1.0 <= limit) {
                allowed[0] = true;
                current++;
            }
            return new Counters(id, current, previous);
        });
        return allowed[0];
    }
}
