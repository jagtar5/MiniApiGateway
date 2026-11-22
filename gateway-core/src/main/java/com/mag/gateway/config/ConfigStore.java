package com.mag.gateway.config;

/** Persistence port (Dependency Inversion): ConfigRepository implements it with JPA/PostgreSQL. */
public interface ConfigStore {

    /** @return the persisted configuration, or {@link ConfigSnapshot#EMPTY} if nothing is stored. */
    ConfigSnapshot load();

    /** Atomically replaces the persisted configuration. Throws on failure; nothing half-written. */
    void save(ConfigSnapshot snapshot);
}
