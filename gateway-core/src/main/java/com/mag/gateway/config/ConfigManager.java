package com.mag.gateway.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Owns the live configuration.
 *
 * <ul>
 *   <li>Readers call {@link #current()}: one volatile read, no locks, never blocked by a reload.</li>
 *   <li>Writers go through {@link #reload}: validate FIRST, then swap with
 *       {@code AtomicReference.set()} (copy-on-write). An invalid candidate throws and the
 *       last-known-good snapshot keeps being served.</li>
 *   <li>Writers are serialised with a lock only so that version numbers stay strictly increasing;
 *       the lock is never taken on the read path.</li>
 * </ul>
 */
public class ConfigManager implements ConfigProvider {

    private static final System.Logger LOG = System.getLogger(ConfigManager.class.getName());

    private final ConfigValidator validator;
    private final AtomicReference<ConfigSnapshot> live = new AtomicReference<>(ConfigSnapshot.EMPTY);
    private final List<Consumer<ConfigSnapshot>> listeners = new CopyOnWriteArrayList<>();

    public ConfigManager(ConfigValidator validator) {
        this.validator = java.util.Objects.requireNonNull(validator, "validator");
    }

    @Override
    public ConfigSnapshot current() {
        return live.get();
    }

    /** Called after every successful swap (e.g. to rebuild the RateLimiterRegistry). */
    public void addListener(Consumer<ConfigSnapshot> listener) {
        listeners.add(listener);
    }

    /** Validates without applying. Used by the Admin API before it touches the database. */
    public void validate(ConfigSnapshot candidate) {
        validator.validateOrThrow(candidate);
    }

    /**
     * Validate -> build new snapshot -> atomic swap.
     * The version of {@code candidate} is ignored; the manager assigns current.version + 1.
     *
     * @throws ConfigValidationException if invalid; the live snapshot is NOT changed
     */
    public synchronized ConfigSnapshot reload(ConfigSnapshot candidate) {
        validator.validateOrThrow(candidate);
        ConfigSnapshot next = new ConfigSnapshot(live.get().version() + 1, candidate.routes(), candidate.rules());
        live.set(next);
        for (Consumer<ConfigSnapshot> l : listeners) {
            try {
                l.accept(next);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "config listener failed: " + e.getMessage());
            }
        }
        return next;
    }

    /** Hot-reload entry point: re-read the persisted config and apply it. */
    public ConfigSnapshot reloadFrom(ConfigStore store) {
        return reload(store.load());
    }
}
