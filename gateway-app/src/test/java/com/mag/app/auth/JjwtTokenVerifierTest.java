package com.mag.app.auth;

import com.mag.core.exception.AuthenticationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JjwtTokenVerifierTest {

    private static final String SECRET = "test-signing-secret-with-32-bytes-min";
    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JjwtTokenVerifier verifier = new JjwtTokenVerifier(SECRET);

    @Test
    void acceptsValidSignedTokenAndReturnsClaims() {
        String token = Jwts.builder()
                .subject("client-42")
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();

        Map<String, Object> claims = verifier.verify(token);

        assertEquals("client-42", claims.get("sub"));
    }

    @Test
    void rejectsExpiredToken() {
        String token = Jwts.builder()
                .subject("client-42")
                .expiration(Date.from(Instant.now().minusSeconds(1)))
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();

        assertThrows(AuthenticationException.class, () -> verifier.verify(token));
    }

    @Test
    void rejectsBadSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "different-signing-secret-with-32-bytes".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("client-42")
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertThrows(AuthenticationException.class, () -> verifier.verify(token));
    }

    @Test
    void rejectsSignedTokensUsingAnAlgorithmOtherThanHs256() {
        String longerSecret = "a".repeat(64);
        SecretKey longerKey = Keys.hmacShaKeyFor(longerSecret.getBytes(StandardCharsets.UTF_8));
        JjwtTokenVerifier hs256Verifier = new JjwtTokenVerifier(longerSecret);
        String token = Jwts.builder()
                .subject("client-42")
                .signWith(longerKey, Jwts.SIG.HS384)
                .compact();

        assertThrows(AuthenticationException.class, () -> hs256Verifier.verify(token));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(AuthenticationException.class, () -> verifier.verify("not-a-jwt"));
    }

    @Test
    void rejectsMissingToken() {
        assertThrows(AuthenticationException.class, () -> verifier.verify(null));
        assertThrows(AuthenticationException.class, () -> verifier.verify("  "));
    }

    @Test
    void rejectsUnsecuredAlgNoneToken() {
        assertThrows(AuthenticationException.class,
                () -> verifier.verify("eyJhbGciOiJub25lIn0.eyJzdWIiOiJhdHRhY2tlciJ9."));
    }
}
