package com.mag.app.config;

import com.mag.core.config.ConfigProvider;
import com.mag.core.config.ConfigPublisher;
import com.mag.core.config.ConfigSnapshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * M-E: Configuration &amp; Admin — AtomicReference snapshot store.
 *
 * Implements both {@link ConfigProvider} (read) and {@link ConfigPublisher} (write)
 * through a single {@link AtomicReference}.
 *
 * Filters see only {@link ConfigProvider}; only the admin module sees {@link ConfigPublisher}.
 * This enforces the ISP split at the Spring injection site.
 *
 * Thread-safety (MAG-QR-04): the {@link AtomicReference} swap is instantaneous;
 * readers always see a complete, consistent snapshot.
 */
public class AtomicConfigStore implements ConfigProvider, ConfigPublisher {

    private final AtomicReference<ConfigSnapshot> current;

    public AtomicConfigStore() {
        // Start with an empty snapshot (version 0); the config manager loads from DB at startup.
        this.current = new AtomicReference<>(new ConfigSnapshot(0L, List.of()));
    }

    @Override
    public ConfigSnapshot getSnapshot() {
        return current.get();
    }

    @Override
    public void publish(ConfigSnapshot snapshot) {
        current.set(snapshot);
    }
}
