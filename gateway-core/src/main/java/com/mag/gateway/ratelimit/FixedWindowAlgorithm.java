package com.mag.gateway.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed window counter: at most {@code limit} requests per aligned window.
 * Cheapest option, but allows up to 2x limit around a window boundary (see EdgeBurstTest).
 */
public final class FixedWindowAlgorithm implements RateLimitAlgorithm {

    private record Window(long windowId, int count) { }

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowAlgorithm(int limit, Duration window, Clock clock) {
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
        final long id = Math.floorDiv(clock.millis(), windowMillis);
        final boolean[] allowed = new boolean[1];
        windows.compute(key, (k, w) -> {
            if (w == null || w.windowId() != id) {
                allowed[0] = true;
                return new Window(id, 1);
            }
            if (w.count() < limit) {
                allowed[0] = true;
                return new Window(id, w.count() + 1);
            }
            return w;
        });
        return allowed[0];
    }
}
