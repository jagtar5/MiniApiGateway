package com.mag.gateway.config;

/** Thrown when an Admin API call names a route/rule that does not exist (maps to HTTP 404). */
public class ConfigNotFoundException extends RuntimeException {
    public ConfigNotFoundException(String message) {
        super(message);
    }
}
