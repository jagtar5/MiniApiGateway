package com.mag.app.auth;

import com.mag.core.auth.TokenVerifier;
import com.mag.core.exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
 */
@Component
public class JjwtTokenVerifier implements TokenVerifier {

    private final JwtParser parser;

    public JjwtTokenVerifier(@Value("${mag.jwt.secret}") String secret) {
        Objects.requireNonNull(secret, "mag.jwt.secret must not be null");
        if (secret.isBlank()) {
            throw new IllegalArgumentException("mag.jwt.secret must not be blank");
        }
        SecretKey signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    @Override
    public Map<String, Object> verify(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Bearer token is required");
        }
        try {
            Jws<Claims> signedClaims = parser.parseSignedClaims(token);
            if (!Jwts.SIG.HS256.getId().equals(signedClaims.getHeader().getAlgorithm())) {
                throw new AuthenticationException("Bearer token uses an unsupported signing algorithm");
            }
            return Collections.unmodifiableMap(new HashMap<>(signedClaims.getPayload()));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthenticationException("Bearer token is invalid", exception);
        }
    }
}
