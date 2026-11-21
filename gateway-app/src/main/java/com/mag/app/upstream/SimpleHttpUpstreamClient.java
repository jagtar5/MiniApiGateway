package com.mag.app.upstream;

import com.mag.core.exception.UpstreamTimeoutException;
import com.mag.core.exception.UpstreamUnavailableException;
import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * M-A / Integration Scaffolding: Real HTTP Upstream Client.
 *
 * Dispatches requests to upstream HTTP endpoints (e.g., dummy-upstreams on port 9090).
 * Enforces connect and read timeouts, mapping network failures to GatewayExceptions (MAG-QR-03).
 */
public class SimpleHttpUpstreamClient implements UpstreamClient {

    private final HttpClient httpClient;
    private final String defaultBaseUrl;
    private final Duration readTimeout;

    public SimpleHttpUpstreamClient(String defaultBaseUrl, Duration connectTimeout, Duration readTimeout) {
        this.defaultBaseUrl = Objects.requireNonNull(defaultBaseUrl, "defaultBaseUrl");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public UpstreamResponse execute(GatewayRequest request) {
        Objects.requireNonNull(request, "request");

        String targetUrl = resolveTargetUrl(request);
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(readTimeout);

        // Forward headers (skipping restricted HTTP/2 pseudo-headers if any)
        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            String key = entry.getKey();
            if (!isRestrictedHeader(key)) {
                reqBuilder.header(key, entry.getValue());
            }
        }

        // Method
        String method = request.getMethod().toUpperCase();
        if ("GET".equals(method)) {
            reqBuilder.GET();
        } else if ("DELETE".equals(method)) {
            reqBuilder.DELETE();
        } else {
            reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        try {
            HttpResponse<String> httpResponse = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            UpstreamResponse.Builder respBuilder = UpstreamResponse.builder()
                    .statusCode(httpResponse.statusCode())
                    .body(httpResponse.body() == null ? "" : httpResponse.body());

            httpResponse.headers().map().forEach((k, vList) -> {
                if (vList != null && !vList.isEmpty()) {
                    respBuilder.header(k, vList.get(0));
                }
            });

            return respBuilder.build();
        } catch (HttpTimeoutException te) {
            throw new UpstreamTimeoutException("Upstream timed out at " + targetUrl + ": " + te.getMessage(), te);
        } catch (IOException ie) {
            throw new UpstreamUnavailableException("Upstream unavailable at " + targetUrl + ": " + ie.getMessage(), ie);
        } catch (InterruptedException ite) {
            Thread.currentThread().interrupt();
            throw new UpstreamUnavailableException("Upstream request interrupted", ite);
        }
    }

    private String resolveTargetUrl(GatewayRequest request) {
        String path = request.getPath();
        // If path starts with /api/, map to dummy upstream endpoint without /api
        // e.g. /api/fast -> http://localhost:9090/fast
        String subPath = path.startsWith("/api/") ? path.substring(4) : path;
        String base = defaultBaseUrl.endsWith("/") ? defaultBaseUrl.substring(0, defaultBaseUrl.length() - 1) : defaultBaseUrl;
        if (!subPath.startsWith("/")) {
            subPath = "/" + subPath;
        }

        StringBuilder sb = new StringBuilder(base).append(subPath);
        if (!request.getQueryParams().isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> qp : request.getQueryParams().entrySet()) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(qp.getKey()).append("=").append(qp.getValue());
                first = false;
            }
        }
        return sb.toString();
    }

    private boolean isRestrictedHeader(String name) {
        return "content-length".equalsIgnoreCase(name)
                || "host".equalsIgnoreCase(name)
                || "connection".equalsIgnoreCase(name);
    }
}
