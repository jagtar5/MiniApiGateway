package com.mag.core.clock;

import java.time.Instant;

/**
 * Time abstraction injected into every time-sensitive component.
 *
 * Dependency Inversion + Testability (MAG-QR-10, R6):
 *   - Core classes depend on this interface, never on {@code System.currentTimeMillis()}.
 *   - Tests inject a {@code FakeClock} that advances time programmatically.
 *   - CI enforces a ban on {@code Thread.sleep} in unit tests.
 *
 * ISP: single method — callers need nothing else.
 */
public interface Clock {

    /**
     * Return the current instant according to this clock.
     *
     * @return current time, never null
     */
    Instant now();
}
