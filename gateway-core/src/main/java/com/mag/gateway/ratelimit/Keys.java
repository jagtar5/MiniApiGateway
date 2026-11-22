package com.mag.gateway.ratelimit;

/** Small shared argument check so all algorithms fail the same way. */
final class Keys {
    private Keys() { }

    static String require(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("rate-limit key must not be blank");
        }
        return key;
    }

    static long windowMillis(java.time.Duration window) {
        long ms = window.toMillis();
        if (ms <= 0) {
            throw new IllegalArgumentException("window must be at least 1 ms");
        }
        return ms;
    }
}
