package com.mag.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyExtractorTest {

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String jwtWithPayload(String payloadJson) {
        return b64("{\"alg\":\"HS256\"}") + "." + b64(payloadJson) + "." + "sig";
    }

    private static RequestView withAuth(String value) {
        return new RequestView(Map.of("authorization", value), "10.0.0.5");
    }

    private final JwtSubjectKeyExtractor jwt = new JwtSubjectKeyExtractor();

    @Test
    void jwtSubjectIsExtracted() {
        String token = jwtWithPayload("{\"sub\":\"alice\",\"exp\":9999999999}");
        assertEquals("sub:alice", jwt.extract(withAuth("Bearer " + token)));
    }

    @Test
    void jwtHeaderNameIsCaseInsensitive() {
        String token = jwtWithPayload("{\"sub\":\"bob\"}");
        RequestView r = new RequestView(Map.of("AUTHORIZATION", "bearer " + token), null);
        assertEquals("sub:bob", jwt.extract(r));
    }

    @Test
    void missingHeaderGivesNull() {
        assertNull(jwt.extract(new RequestView(Map.of(), "1.2.3.4")));
    }

    @Test
    void nonBearerSchemeGivesNull() {
        assertNull(jwt.extract(withAuth("Basic abc")));
    }

    @Test
    void malformedTokenGivesNullNotException() {
        assertNull(jwt.extract(withAuth("Bearer notajwt")));
        assertNull(jwt.extract(withAuth("Bearer a.!!!notbase64!!!.c")));
    }

    @Test
    void tokenWithoutSubGivesNull() {
        assertNull(jwt.extract(withAuth("Bearer " + jwtWithPayload("{\"name\":\"x\"}"))));
    }

    @Test
    void clientIpUsesRemoteAddressByDefault() {
        ClientIpKeyExtractor ip = new ClientIpKeyExtractor();
        RequestView r = new RequestView(Map.of("X-Forwarded-For", "6.6.6.6"), "10.0.0.5");
        assertEquals("ip:10.0.0.5", ip.extract(r));   // spoofed header ignored
    }

    @Test
    void clientIpHonoursForwardedForOnlyWhenTrusted() {
        ClientIpKeyExtractor ip = new ClientIpKeyExtractor(true);
        RequestView r = new RequestView(Map.of("X-Forwarded-For", "6.6.6.6, 10.0.0.1"), "10.0.0.5");
        assertEquals("ip:6.6.6.6", ip.extract(r));
    }

    @Test
    void clientIpNullWhenNothingKnown() {
        assertNull(new ClientIpKeyExtractor().extract(new RequestView(Map.of(), null)));
    }
}
