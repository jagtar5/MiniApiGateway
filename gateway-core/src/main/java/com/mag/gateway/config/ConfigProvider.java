package com.mag.gateway.config;

/**
 * Read-only access to the live configuration. Ahmad publishes the official ConfigProvider in
 * P0-1: if it already exists on main, delete this file and keep the method name {@code current()}.
 */
public interface ConfigProvider {
    ConfigSnapshot current();
}
