package com.mag.dummy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides the four test fixture endpoints used by the MAG integration tests.
 *
 *   GET /fast      — immediate 200 (baseline latency test)
 *   GET /slow      — delayed response (timeout / MAG-QR-03 testing)
 *   GET /flaky     — alternates between 200 and 500 (circuit-breaker / MAG-QR-02 testing)
 *   GET /cacheable — cacheable 200 response (cache / MAG-FR-03 testing)
 */
@RestController
public class DummyUpstreamController {

    private static final long SLOW_DELAY_MS = 10_000; // longer than gateway timeout
    private static final double FLAKY_FAILURE_RATE = 0.5; // 50% failures

    private final AtomicInteger requestCount = new AtomicInteger(0);

    /** Immediately returns 200 with a small JSON body. */
    @GetMapping("/fast")
    public ResponseEntity<Map<String, Object>> fast() {
        return ResponseEntity.ok(Map.of(
                "endpoint", "fast",
                "request", requestCount.incrementAndGet()
        ));
    }

    /** Sleeps for {@value #SLOW_DELAY_MS} ms to trigger the gateway's timeout (MAG-QR-03). */
    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> slow() throws InterruptedException {
        Thread.sleep(SLOW_DELAY_MS);
        return ResponseEntity.ok(Map.of("endpoint", "slow"));
    }

    /** Randomly returns 200 or 500 to exercise the circuit breaker (MAG-QR-02). */
    @GetMapping("/flaky")
    public ResponseEntity<Map<String, Object>> flaky() {
        if (ThreadLocalRandom.current().nextDouble() < FLAKY_FAILURE_RATE) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "simulated upstream failure"));
        }
        return ResponseEntity.ok(Map.of("endpoint", "flaky", "ok", true));
    }

    /** Returns 200 with cache headers for GET-cache testing (MAG-FR-03). */
    @GetMapping("/cacheable")
    public ResponseEntity<Map<String, Object>> cacheable() {
        return ResponseEntity.ok()
                .header("Cache-Control", "max-age=60")
                .body(Map.of("endpoint", "cacheable", "timestamp", System.currentTimeMillis()));
    }
}
