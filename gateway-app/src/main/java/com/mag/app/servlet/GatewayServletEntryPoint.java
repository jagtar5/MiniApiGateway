package com.mag.app.servlet;

import com.mag.app.config.AtomicConfigStore;
import com.mag.app.metrics.GatewayMetrics;
import com.mag.core.config.RouteConfig;
import com.mag.core.exception.CircuitOpenException;
import com.mag.core.exception.GatewayException;
import com.mag.core.exception.GatewayExceptionMapper;
import com.mag.core.exception.UpstreamTimeoutException;
import com.mag.core.exception.UpstreamUnavailableException;
import com.mag.core.model.GatewayRequest;
import com.mag.core.model.GatewayResponse;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;
import com.mag.core.upstream.impl.CachingUpstreamClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

/**
 * Servlet entry point — maps incoming gateway requests to the upstream decorator chain.
 *
 * Responsibilities:
 *   1. Converts HttpServletRequest → GatewayRequest.
 *   2. Routes request matching active ConfigSnapshot.
 *   3. Enforces basic auth check if route.isAuthRequired() (scaffolding until M-B AuthFilter merges).
 *   4. Executes via CachingUpstreamClient → ResilientUpstreamClient → SimpleHttpUpstreamClient.
 *   5. Updates live GatewayMetrics and writes response back.
 */
@Controller
@RequestMapping({"/api/**", "/gateway/**"})
public class GatewayServletEntryPoint {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final UpstreamClient upstreamClient;
    private final AtomicConfigStore configStore;
    private final GatewayMetrics metrics;

    public GatewayServletEntryPoint(
            @org.springframework.beans.factory.annotation.Qualifier("upstreamClient") UpstreamClient upstreamClient,
            AtomicConfigStore configStore,
            GatewayMetrics metrics) {
        this.upstreamClient = upstreamClient;
        this.configStore = configStore;
        this.metrics = metrics;
    }

    @RequestMapping
    public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 1. Build GatewayRequest from HttpServletRequest
        GatewayRequest.Builder reqBuilder = GatewayRequest.builder()
                .method(method)
                .path(path);

        // Headers
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        boolean hasRequestId = false;
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = httpRequest.getHeader(name);
                reqBuilder.header(name, value);
                if (HEADER_REQUEST_ID.equalsIgnoreCase(name)) {
                    hasRequestId = true;
                }
            }
        }
        if (!hasRequestId) {
            reqBuilder.header(HEADER_REQUEST_ID, UUID.randomUUID().toString());
        }

        // Query parameters
        for (Map.Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                reqBuilder.queryParam(entry.getKey(), entry.getValue()[0]);
            }
        }

        GatewayRequest gatewayRequest = reqBuilder.build();

        // 2. Find matching route from ConfigSnapshot
        RouteConfig matchingRoute = findMatchingRoute(path, method);
        if (matchingRoute == null) {
            httpResponse.setStatus(404);
            httpResponse.getWriter().write("{\"error\": \"404 Not Found: No matching route for " + path + "\"}");
            return;
        }

        // 3. Auth requirement check
        if (matchingRoute.isAuthRequired()) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                metrics.recordBlocked();
                httpResponse.setStatus(401);
                httpResponse.setHeader("Content-Type", "application/json");
                httpResponse.getWriter().write("{\"error\": \"401 Unauthorized: Bearer JWT token required\"}");
                return;
            }
        }

        // 4. Execute through decorator chain (Caching -> Resilient -> Upstream)
        try {
            boolean wasCached = false;
            if (upstreamClient instanceof CachingUpstreamClient cachingClient) {
                if ("GET".equalsIgnoreCase(method)) {
                    // Pre-check if entry was already present to count cache hits
                    wasCached = cachingClient.getCache().containsKey(
                            com.mag.core.cache.CacheKey.fromRequest(gatewayRequest)
                    );
                }
            }

            UpstreamResponse upstreamResponse = upstreamClient.execute(gatewayRequest);

            if (wasCached) {
                metrics.recordCacheHit();
            } else {
                metrics.recordCacheMiss();
                metrics.recordAllowed();
            }

            // Write response
            httpResponse.setStatus(upstreamResponse.getStatusCode());
            for (Map.Entry<String, String> header : upstreamResponse.getHeaders().entrySet()) {
                httpResponse.setHeader(header.getKey(), header.getValue());
            }
            httpResponse.getWriter().write(upstreamResponse.getBody());

        } catch (CircuitOpenException coe) {
            metrics.recordBlocked();
            httpResponse.setStatus(503);
            httpResponse.setHeader("Content-Type", "application/json");
            httpResponse.getWriter().write("{\"error\": \"503 Service Unavailable: Circuit breaker is OPEN (Fail-fast)\"}");
        } catch (UpstreamTimeoutException ute) {
            metrics.recordBlocked();
            httpResponse.setStatus(504);
            httpResponse.setHeader("Content-Type", "application/json");
            httpResponse.getWriter().write("{\"error\": \"504 Gateway Timeout: " + ute.getMessage() + "\"}");
        } catch (UpstreamUnavailableException uue) {
            metrics.recordBlocked();
            httpResponse.setStatus(502);
            httpResponse.setHeader("Content-Type", "application/json");
            httpResponse.getWriter().write("{\"error\": \"502 Bad Gateway: " + uue.getMessage() + "\"}");
        } catch (GatewayException ge) {
            metrics.recordBlocked();
            GatewayResponse gr = GatewayExceptionMapper.toResponse(ge);
            httpResponse.setStatus(gr.getStatusCode());
            httpResponse.getWriter().write(gr.getBody());
        } catch (Exception e) {
            metrics.recordBlocked();
            httpResponse.setStatus(500);
            httpResponse.getWriter().write("{\"error\": \"500 Internal Server Error: " + e.getMessage() + "\"}");
        }
    }

    private RouteConfig findMatchingRoute(String path, String method) {
        for (RouteConfig route : configStore.getSnapshot().getRoutes()) {
            String pattern = route.getPathPattern();
            if (matchesPattern(path, pattern)) {
                if (route.getMethods().isEmpty() || route.getMethods().contains(method.toUpperCase())) {
                    return route;
                }
            }
        }
        return null;
    }

    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }
}
