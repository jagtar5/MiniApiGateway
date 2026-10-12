package com.mag.core.exception;

/** Thrown when a configuration payload fails validation (MAG-QR-04). */
public class InvalidConfigurationException extends GatewayException {
    public InvalidConfigurationException(String fieldName, String reason) {
        super(400, "Invalid configuration — field '" + fieldName + "': " + reason);
    }
}
