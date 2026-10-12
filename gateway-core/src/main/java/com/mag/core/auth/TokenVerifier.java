package com.mag.core.auth;

import com.mag.core.exception.AuthenticationException;

import java.util.Map;

/**
 * M-B: Identity &amp; Traffic Control
 *
 * Contract for JWT verification (ISP: one focused method).
 * The concrete implementation lives in gateway-app so the core has
 * no dependency on any JWT library at compile time (DIP).
 *
 * Implementations MUST reject:
 *   - missing / malformed tokens
 *   - expired tokens
 *   - tokens with alg=none
 *   - tokens with an invalid signature
 * (MAG-QR-05)
 */
public interface TokenVerifier {

    /**
     * Verify {@code token} and return its claims on success.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return a read-only map of claim names to values
     * @throws AuthenticationException if the token is invalid for any reason
     */
    Map<String, Object> verify(String token);
}
