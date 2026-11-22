package com.mag.app.servlet;

import com.mag.app.config.AtomicConfigStore;
import com.mag.app.metrics.GatewayMetrics;
import com.mag.core.config.RouteConfig;
import com.mag.core.auth.TokenVerifier;
import com.mag.core.exception.GatewayException;
import com.mag.core.exception.GatewayExceptionMapper;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.PipelineBuilder;
import com.mag.core.filter.impl.AuthFilter;
import com.mag.core.filter.impl.ForwardFilter;
import com.mag.core.filter.impl.RequestIdFilter;
import com.mag.core.model.GatewayRequest;
import com.mag.core.model.GatewayResponse;
import com.mag.core.upstream.UpstreamClient;
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
 *   3. Builds the request-ID, authentication and forwarding filter pipeline.
 *   4. Updates live GatewayMetrics and writes response back.
 */
@Controller
@RequestMapping({"/api/**", "/gateway/**"})
public class GatewayServletEntryPoint {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final UpstreamClient upstreamClient;
    private final AtomicConfigStore configStore;
    private final GatewayMetrics metrics;
    private final TokenVerifier tokenVerifier;

    public GatewayServletEntryPoint(
            @org.springframework.beans.factory.annotation.Qualifier("upstreamClient") UpstreamClient upstreamClient,
            AtomicConfigStore configStore,
            GatewayMetrics metrics,
            TokenVerifier tokenVerifier) {
        this.upstreamClient = upstreamClient;
        this.configStore = configStore;
        this.metrics = metrics;
        this.tokenVerifier = tokenVerifier;
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
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = httpRequest.getHeader(name);
                reqBuilder.header(name, value);
            }
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
            httpResponse.setHeader(HEADER_REQUEST_ID, UUID.randomUUID().toString());
            httpResponse.getWriter().write("{\"error\": \"404 Not Found: No matching route for " + path + "\"}");
            return;
        }

        // 3. Execute through the Chain of Responsibility (request ID → optional auth → forwarding).
        GatewayContext context = new GatewayContext(gatewayRequest);
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

            new PipelineBuilder(matchingRoute)
                    .withRequestId(new RequestIdFilter())
                    .withAuth(new AuthFilter(tokenVerifier))
                    .withForward(new ForwardFilter(upstreamClient))
                    .build()
                    .proceed(context);

            GatewayResponse response = context.getResponse();
            if (response == null) {
                throw new IllegalStateException("Gateway pipeline completed without a response");
            }

            if (wasCached) {
                metrics.recordCacheHit();
            } else {
                metrics.recordCacheMiss();
                metrics.recordAllowed();
            }

            writeResponse(httpResponse, response, context.getAttribute(RequestIdFilter.HEADER_REQUEST_ID));
        } catch (GatewayException ge) {
            metrics.recordBlocked();
            writeResponse(
                    httpResponse,
                    GatewayExceptionMapper.toResponse(ge),
                    context.getAttribute(RequestIdFilter.HEADER_REQUEST_ID));
        }
    }

    private void writeResponse(HttpServletResponse httpResponse, GatewayResponse response, String requestId)
            throws IOException {
        httpResponse.setStatus(response.getStatusCode());
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            httpResponse.setHeader(header.getKey(), header.getValue());
        }
        if (requestId != null) {
            httpResponse.setHeader(HEADER_REQUEST_ID, requestId);
        }
        httpResponse.getWriter().write(response.getBody());
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
