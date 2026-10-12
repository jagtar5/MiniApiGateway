package com.mag.app.auth;

import com.mag.core.auth.TokenVerifier;
import com.mag.core.exception.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * M-B: Identity &amp; Traffic Control — concrete JWT verifier (Spring shell).
 *
 * Implements the core's {@link TokenVerifier} interface using the JJWT library.
 * Lives in gateway-app because JJWT is a runtime dependency not allowed in gateway-core.
 *
 * Rejects all invalid token classes (MAG-QR-05):
 *   - Missing or malformed tokens
 *   - Expired tokens
 *   - alg=none tokens
 *   - Badly signed tokens
 *
 * Tokens and secrets NEVER appear in logs (MAG-QR-06).
 *
 * TODO M2: inject signing key via @Value("${mag.jwt.secret}") and configure JJWT parser.
 */
@Component
public class JjwtTokenVerifier implements TokenVerifier {

    // TODO M2: inject JwtParser configured with pinned algorithm and signing key

    @Override
    public Map<String, Object> verify(String token) {
        // TODO M2: parse and validate token using JJWT;
        //          throw AuthenticationException (NOT a logged stack trace with the token) on failure.
        throw new UnsupportedOperationException("JjwtTokenVerifier.verify — implement in M2");
    }
}
