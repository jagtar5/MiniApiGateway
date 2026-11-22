package com.mag.gateway.ratelimit;

import java.util.Map;
import java.util.TreeMap;

/**
 * The only facts the rate limiter needs from a request. Decouples this module from the
 * concrete GatewayRequest type (Dependency Inversion); the filter adapter builds one of these.
 */
public record RequestView(Map<String, String> headers, String remoteAddress) {

    public RequestView {
        Map<String, String> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    ci.put(k, v);
                }
            });
        }
        headers = java.util.Collections.unmodifiableMap(ci);
    }

    public String header(String name) {
        return headers.get(name);
    }
}
