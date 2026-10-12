package com.mag.core.config;

/**
 * M-E: Configuration &amp; Admin — ISP
 *
 * Read-only view of the current active {@link ConfigSnapshot}.
 * Exposed to all filters so they can read routing and rate-limit rules.
 *
 * ISP: filters must NOT be able to trigger a reload or write to config.
 * Only the admin module sees {@link ConfigPublisher}.
 */
public interface ConfigProvider {

    /**
     * Return the currently active configuration snapshot.
     * Always returns a non-null, consistent snapshot (never a partially written one).
     *
     * @return the active {@link ConfigSnapshot}
     */
    ConfigSnapshot getSnapshot();
}
