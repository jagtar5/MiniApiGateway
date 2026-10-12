package com.mag.core.exception;

/** 403 Forbidden — valid token but insufficient permissions. */
public class AuthorizationException extends GatewayException {
    public AuthorizationException(String message) { super(403, message); }
}
