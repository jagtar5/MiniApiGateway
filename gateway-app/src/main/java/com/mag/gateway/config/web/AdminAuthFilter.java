package com.mag.gateway.config.web;

import com.mag.gateway.config.AdminCredentialChecker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Guards /admin/**. 401 = no credential, 403 = wrong credential (or admin API not configured). */
public class AdminAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Token";

    private final AdminCredentialChecker checker;

    public AdminAuthFilter(AdminCredentialChecker checker) {
        this.checker = checker;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        switch (checker.check(req.getHeader(HEADER))) {
            case OK -> chain.doFilter(req, res);
            case UNAUTHORIZED -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "admin credential required");
            case FORBIDDEN -> res.sendError(HttpServletResponse.SC_FORBIDDEN, "invalid admin credential");
        }
    }
}
