package com.mag.app.upstream;

import com.mag.core.config.RouteConfig;
import com.mag.core.filter.GatewayContext;
import com.mag.core.filter.PipelineBuilder;
import com.mag.core.filter.impl.ForwardFilter;
import com.mag.core.model.GatewayRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleHttpUpstreamClientLspContractTest {

    @Test
    void forwardsTheRealHttpClientResponseThroughForwardFilter() throws IOException {
        byte[] responseBody = "local upstream response".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/contract", exchange -> {
            exchange.sendResponseHeaders(202, responseBody.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(responseBody);
            }
        });
        server.start();

        try {
            SimpleHttpUpstreamClient client = new SimpleHttpUpstreamClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(2));
            GatewayContext context = new GatewayContext(GatewayRequest.builder()
                    .method("GET")
                    .path("/contract")
                    .build());
            RouteConfig route = RouteConfig.builder().id("contract").pathPattern("/contract").build();

            new PipelineBuilder(route)
                    .withForward(new ForwardFilter(client))
                    .build()
                    .proceed(context);

            assertEquals(202, context.getResponse().getStatusCode());
            assertEquals("local upstream response", context.getResponse().getBody());
        } finally {
            server.stop(0);
        }
    }
}
