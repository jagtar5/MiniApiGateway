package com.mag.gateway.ratelimit;

/**
 * Keys requests by client IP. X-Forwarded-For is spoofable by clients, so it is only honoured
 * when the gateway is known to sit behind a trusted proxy ({@code trustForwardedFor = true}).
 */
public final class ClientIpKeyExtractor implements KeyExtractor {

    private final boolean trustForwardedFor;

    public ClientIpKeyExtractor() {
        this(false);
    }

    public ClientIpKeyExtractor(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    public String extract(RequestView request) {
        if (trustForwardedFor) {
            String xff = request.header("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String first = xff.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return "ip:" + first;
                }
            }
        }
        String remote = request.remoteAddress();
        return (remote == null || remote.isBlank()) ? null : "ip:" + remote.trim();
    }
}
