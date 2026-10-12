package com.mag.core.clock;

import java.time.Instant;

/**
 * Production {@link Clock} that delegates to {@link Instant#now()}.
 * Registered as a singleton bean in the Spring shell.
 */
public final class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
