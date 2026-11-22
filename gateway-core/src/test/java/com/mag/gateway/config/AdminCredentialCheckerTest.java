package com.mag.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Admin API security: calls without the admin credential get 401/403 (see AdminAuthFilter). */
class AdminCredentialCheckerTest {

    private final AdminCredentialChecker checker = new AdminCredentialChecker("s3cret-token");

    @Test
    void missingCredentialIsUnauthorized401() {
        assertEquals(AdminCredentialChecker.Result.UNAUTHORIZED, checker.check(null));
        assertEquals(AdminCredentialChecker.Result.UNAUTHORIZED, checker.check(""));
        assertEquals(AdminCredentialChecker.Result.UNAUTHORIZED, checker.check("   "));
    }

    @Test
    void wrongCredentialIsForbidden403() {
        assertEquals(AdminCredentialChecker.Result.FORBIDDEN, checker.check("wrong"));
        assertEquals(AdminCredentialChecker.Result.FORBIDDEN, checker.check("s3cret-token "));
    }

    @Test
    void correctCredentialIsAccepted() {
        assertEquals(AdminCredentialChecker.Result.OK, checker.check("s3cret-token"));
    }

    @Test
    void unconfiguredTokenFailsClosed() {
        assertEquals(AdminCredentialChecker.Result.FORBIDDEN, new AdminCredentialChecker(null).check("anything"));
        assertEquals(AdminCredentialChecker.Result.FORBIDDEN, new AdminCredentialChecker("").check("anything"));
    }
}
