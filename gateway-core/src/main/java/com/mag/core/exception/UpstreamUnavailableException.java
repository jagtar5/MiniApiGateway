package com.mag.core.exception;

/** 502 Bad Gateway — upstream returned an unexpected/error response. */
public class UpstreamUnavailableException extends GatewayException {
    public UpstreamUnavailableException(String message) { super(502, message); }
    public UpstreamUnavailableException(String message, Throwable cause) { super(502, message, cause); }
}
