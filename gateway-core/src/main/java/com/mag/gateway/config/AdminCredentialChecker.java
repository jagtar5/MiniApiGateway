package com.mag.gateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Decides whether an Admin API call carries the admin credential.
 * The expected token comes from configuration/environment (never from source code) and is
 * compared in constant time. If no token is configured the API is locked (fail closed).
 */
public final class AdminCredentialChecker {

    public enum Result { OK, UNAUTHORIZED, FORBIDDEN }

    private final byte[] expected;

    public AdminCredentialChecker(String expectedToken) {
        this.expected = (expectedToken == null || expectedToken.isBlank())
                ? null : expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    /** @param presented value of the X-Admin-Token header (may be null) */
    public Result check(String presented) {
        if (presented == null || presented.isBlank()) {
            return Result.UNAUTHORIZED;   // 401: no credential supplied
        }
        if (expected == null) {
            return Result.FORBIDDEN;      // 403: admin API not configured -> nobody gets in
        }
        boolean same = MessageDigest.isEqual(expected, presented.getBytes(StandardCharsets.UTF_8));
        return same ? Result.OK : Result.FORBIDDEN;
    }
}
