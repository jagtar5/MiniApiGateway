package com.mag.core.exception;

import com.mag.core.model.GatewayResponse;

/**
 * M-A: Proxy Pipeline — Single mapping point (MAG-FR-04).
 *
 * Converts any {@link GatewayException} subclass into the correct
 * {@link GatewayResponse}.  No other class in the core performs this
 * conversion; no generic {@code catch (Exception)} exists elsewhere.
 *
 * SRP: this class has exactly one responsibility — exception-to-HTTP mapping.
 */
public final class GatewayExceptionMapper {

    private GatewayExceptionMapper() { /* utility */ }

    /**
     * Map a {@link GatewayException} to a {@link GatewayResponse}.
     *
     * @param ex the exception thrown by any filter or upstream client
     * @return the appropriate HTTP response (401, 403, 429, 502, 503, 504, etc.)
     */
    public static GatewayResponse toResponse(GatewayException ex) {
        if (ex instanceof RateLimitExceededException rle) {
            return GatewayResponse.tooManyRequests(rle.getRetryAfterSeconds());
        }
        // All other subclasses carry their status code directly.
        return GatewayResponse.builder()
                .statusCode(ex.getHttpStatus())
                .body(ex.getMessage())
                .build();
    }
}
