package com.mag.core.cache;

import com.mag.core.model.GatewayRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * M-C: Fast-Path Cache — User-Safe Cache Key.
 *
 * Responsibilities:
 *   - Uniquely identifies a cacheable resource based on HTTP method, path, query parameters,
 *     and user scope (MAG-FR-03).
 *   - User isolation (MAG-QR-06): extracts client identity from Authorization or X-User-Id headers
 *     so one user's response is never returned to another.
 *   - Secret protection: Authorization tokens are hashed with SHA-256 and masked in {@link #toString()}
 *     to prevent tokens from appearing in logs or memory dumps.
 */
public final class CacheKey {

    private final String method;
    private final String path;
    private final Map<String, String> queryParams;
    private final String userScope;

    public CacheKey(String method, String path, Map<String, String> queryParams, String userScope) {
        this.method = Objects.requireNonNull(method, "method").toUpperCase();
        this.path = Objects.requireNonNull(path, "path");
        this.queryParams = (queryParams == null || queryParams.isEmpty())
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new TreeMap<>(queryParams));
        this.userScope = (userScope == null || userScope.isBlank()) ? "public" : userScope;
    }

    public static CacheKey fromRequest(GatewayRequest request) {
        Objects.requireNonNull(request, "request");
        String userScope = extractUserScope(request);
        return new CacheKey(request.getMethod(), request.getPath(), request.getQueryParams(), userScope);
    }

    private static String extractUserScope(GatewayRequest request) {
        String userId = getHeaderCaseInsensitive(request, "X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId.trim();
        }
        String authHeader = getHeaderCaseInsensitive(request, "Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            return "auth:" + sha256Hex(authHeader.trim());
        }
        return "public";
    }

    private static String getHeaderCaseInsensitive(GatewayRequest request, String headerName) {
        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getUserScope() {
        return userScope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheKey other)) {
            return false;
        }
        return method.equals(other.method)
                && path.equals(other.path)
                && queryParams.equals(other.queryParams)
                && userScope.equals(other.userScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, queryParams, userScope);
    }

    @Override
    public String toString() {
        String displayScope = "public".equals(userScope) ? "public" : "[USER_SCOPED]";
        return "CacheKey{" + method + " " + path + ", params=" + queryParams + ", scope=" + displayScope + "}";
    }
}
