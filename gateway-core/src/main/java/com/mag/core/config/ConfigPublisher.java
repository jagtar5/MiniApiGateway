package com.mag.core.config;

/**
 * M-E: Configuration &amp; Admin — ISP
 *
 * Write interface for publishing a new validated {@link ConfigSnapshot}.
 * Visible only to the admin module (config manager); filters see only {@link ConfigProvider}.
 *
 * The implementation in the Spring shell uses an {@code AtomicReference} so
 * the swap is atomic and in-flight requests see a consistent snapshot (MAG-QR-04).
 */
public interface ConfigPublisher {

    /**
     * Atomically replace the active snapshot with a new validated one.
     * Must never be called with an invalid snapshot — validate first via
     * {@link ConfigValidator}.
     *
     * @param snapshot the new, fully validated configuration
     */
    void publish(ConfigSnapshot snapshot);
}
