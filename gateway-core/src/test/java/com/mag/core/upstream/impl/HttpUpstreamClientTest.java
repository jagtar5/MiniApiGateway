package com.mag.core.upstream.impl;

import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpUpstreamClientTest {

    @Test
    void executeReturnsNotImplementedStub() {
        HttpUpstreamClient client = new HttpUpstreamClient(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));

        GatewayRequest request = GatewayRequest.builder()
                .method("GET")
                .path("/api/fast")
                .build();

        UpstreamResponse response = client.execute(request);

        assertEquals(501, response.getStatusCode());
        assertEquals("Not Implemented", response.getBody());
    }
}
