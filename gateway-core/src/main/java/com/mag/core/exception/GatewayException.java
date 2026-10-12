package com.mag.core.exception;

/**
 * Root of the MAG exception hierarchy.
 *
 * Every module throws a subclass of this; the central
 * {@code GatewayExceptionMapper} in M-A converts them to the correct
 * HTTP status code (MAG-FR-04).
 *
 * No generic {@code catch (Exception)} is used elsewhere in the core.
 */
public class GatewayException extends RuntimeException {

    private final int httpStatus;

    protected GatewayException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    protected GatewayException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
