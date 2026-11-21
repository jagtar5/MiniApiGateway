package com.mag.core.cache;

import com.mag.core.clock.Clock;
import com.mag.core.upstream.UpstreamResponse;

import java.time.Instant;
import java.util.Objects;

/**
 * M-C: Fast-Path Cache — Cache Entry.
 *
 * Immutable wrapper holding an {@link UpstreamResponse} and its expiration instant.
 * Immutability ensures responses are safely shared across threads without mutation hazards (MAG-QR-06).
 */
public final class CacheEntry {

    private final UpstreamResponse response;
    private final Instant expiresAt;

    public CacheEntry(UpstreamResponse response, Instant expiresAt) {
        this.response = Objects.requireNonNull(response, "response");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public UpstreamResponse getResponse() {
        return response;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Checks whether this cache entry is expired relative to the given clock.
     *
     * @param clock injected clock (MAG-QR-10)
     * @return true if clock.now() is at or after expiresAt
     */
    public boolean isExpired(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return !clock.now().isBefore(expiresAt);
    }
}
