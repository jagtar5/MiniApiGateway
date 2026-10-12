package com.mag.core.config;

import com.mag.core.exception.InvalidConfigurationException;

/**
 * M-E: Configuration &amp; Admin
 *
 * Validates a {@link ConfigSnapshot} before it is published.
 * An invalid snapshot must be rejected and the previous one must remain active (MAG-QR-04).
 *
 * SRP: validation logic only — no database or HTTP concerns here.
 * Any failure throws {@link InvalidConfigurationException} naming the field and reason (MAG-QR-13).
 */
public interface ConfigValidator {

    /**
     * Validate the given snapshot.
     *
     * @param snapshot the snapshot to validate
     * @throws InvalidConfigurationException if any field is invalid
     */
    void validate(ConfigSnapshot snapshot);
}
