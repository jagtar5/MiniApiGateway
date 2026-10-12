package com.mag.core.clock;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test-only {@link Clock} that starts at a given {@link Instant}
 * and advances only when {@link #advance(Duration)} is called.
 *
 * Eliminates {@code Thread.sleep} from all time-sensitive unit tests (MAG-QR-10, R6).
 * Must never be used in production code.
 */
public final class FakeClock implements Clock {

    private final AtomicReference<Instant> current;

    public FakeClock(Instant start) {
        this.current = new AtomicReference<>(start);
    }

    public static FakeClock startingNow() {
        return new FakeClock(Instant.now());
    }

    /** Advance this clock by the given duration. */
    public void advance(Duration duration) {
        current.updateAndGet(t -> t.plus(duration));
    }

    @Override
    public Instant now() {
        return current.get();
    }
}
