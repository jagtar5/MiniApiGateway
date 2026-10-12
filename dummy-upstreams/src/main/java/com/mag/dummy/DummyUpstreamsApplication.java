package com.mag.dummy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dummy upstream server for MAG integration and stress testing.
 * Provides four endpoints (SCD Section 2.3 — test fixtures):
 *
 *   GET /fast     — responds immediately with 200
 *   GET /slow     — sleeps for a configurable duration, then responds (simulates timeout)
 *   GET /flaky    — responds 200 or 500 on a configurable failure rate (simulates instability)
 *   GET /cacheable — responds 200 with Cache-Control: max-age header (cache hit testing)
 */
@SpringBootApplication
public class DummyUpstreamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DummyUpstreamsApplication.class, args);
    }
}
