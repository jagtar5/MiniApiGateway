package com.mag.gateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keys requests by the JWT "sub" claim.
 *
 * <p>IMPORTANT: this does NOT verify the signature. It must only run after AuthFilter has
 * validated the token (pipeline order: RequestId -> Auth -> RateLimit -> Forward).
 * The token itself is never logged.
 */
public final class JwtSubjectKeyExtractor implements KeyExtractor {

    private static final Pattern SUB = Pattern.compile("\"sub\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final String BEARER = "Bearer ";

    @Override
    public String extract(RequestView request) {
        String auth = request.header("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String[] parts = auth.substring(BEARER.length()).trim().split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher m = SUB.matcher(payload);
            if (m.find() && !m.group(1).isBlank()) {
                return "sub:" + m.group(1);
            }
        } catch (IllegalArgumentException malformedBase64) {
            return null;
        }
        return null;
    }
}
