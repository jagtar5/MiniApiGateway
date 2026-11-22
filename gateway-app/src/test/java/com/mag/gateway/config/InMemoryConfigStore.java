package com.mag.gateway.config;

/** Test double for the database. Can be told to fail, to prove "last known good" behaviour. */
final class InMemoryConfigStore implements ConfigStore {
    private volatile ConfigSnapshot stored = ConfigSnapshot.EMPTY;
    private volatile boolean failOnSave;

    void failOnSave(boolean fail) { this.failOnSave = fail; }

    @Override public ConfigSnapshot load() { return stored; }

    @Override public void save(ConfigSnapshot snapshot) {
        if (failOnSave) {
            throw new IllegalStateException("simulated database failure");
        }
        stored = snapshot;
    }
}
