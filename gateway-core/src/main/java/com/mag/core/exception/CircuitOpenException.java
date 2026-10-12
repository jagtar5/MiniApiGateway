package com.mag.core.exception;

/** 503 Service Unavailable — circuit breaker is OPEN; call rejected fast (MAG-QR-02). */
public class CircuitOpenException extends GatewayException {
    public CircuitOpenException() { super(503, "Circuit breaker is OPEN"); }
}
