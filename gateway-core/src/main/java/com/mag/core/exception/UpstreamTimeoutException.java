package com.mag.core.exception;

/** 504 Gateway Timeout — upstream did not respond within the configured deadline (MAG-QR-03). */
public class UpstreamTimeoutException extends GatewayException {
    public UpstreamTimeoutException(String message) { super(504, message); }
    public UpstreamTimeoutException(String message, Throwable cause) { super(504, message, cause); }
}
