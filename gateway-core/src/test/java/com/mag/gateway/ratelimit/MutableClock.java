package com.mag.gateway.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/** Fake clock for deterministic time-based tests (no Thread.sleep). */
final class MutableClock extends Clock {
    private final AtomicLong millis;

    MutableClock(long startMillis) {
        this.millis = new AtomicLong(startMillis);
    }

    void advance(long deltaMillis) {
        millis.addAndGet(deltaMillis);
    }

    void set(long absoluteMillis) {
        millis.set(absoluteMillis);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    @Override public long millis() { return millis.get(); }
}
