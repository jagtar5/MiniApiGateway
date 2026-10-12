package com.mag.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Mini API Gateway.
 *
 * The shell's only jobs:
 *   - Bootstrap Spring context (DI wiring, JPA, Flyway, Actuator).
 *   - Convert incoming {@code HttpServletRequest} to {@link com.mag.core.model.GatewayRequest}.
 *   - Hand each request to our own filter chain (not Spring's).
 *   - Convert the resulting {@link com.mag.core.model.GatewayResponse} back to HTTP.
 *
 * No business logic lives here — it all lives in gateway-core.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
