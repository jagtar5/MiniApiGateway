package com.mag.core.exception;

/** 401 Unauthorized — expired, malformed, missing, alg=none tokens (MAG-QR-05). */
public class AuthenticationException extends GatewayException {
    public AuthenticationException(String message) { super(401, message); }
    public AuthenticationException(String message, Throwable cause) { super(401, message, cause); }
}
