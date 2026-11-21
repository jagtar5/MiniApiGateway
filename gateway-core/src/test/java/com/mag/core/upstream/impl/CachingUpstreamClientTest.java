package com.mag.core.upstream.impl;

import com.mag.core.cache.BoundedLruCache;
import com.mag.core.cache.CacheEntry;
import com.mag.core.cache.CacheKey;
import com.mag.core.clock.FakeClock;
import com.mag.core.model.GatewayRequest;
import com.mag.core.upstream.UpstreamClient;
import com.mag.core.upstream.UpstreamResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachingUpstreamClientTest {

    private UpstreamClient mockDelegate;
    private FakeClock fakeClock;
    private Instant startTime;

    @BeforeEach
    void setUp() {
        mockDelegate = mock(UpstreamClient.class);
        startTime = Instant.parse("2026-11-21T12:00:00Z");
        fakeClock = new FakeClock(startTime);
    }

    @Test
    @DisplayName("Cache hit/miss: second identical GET returns cached response without calling upstream (MAG-FR-03)")
    void testCacheHitAndMiss() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest request = GatewayRequest.builder()
                .method("GET")
                .path("/api/items")
                .queryParam("category", "books")
                .build();

        UpstreamResponse expected = UpstreamResponse.builder()
                .statusCode(200)
                .body("{\"items\": [1, 2, 3]}")
                .header("Content-Type", "application/json")
                .build();

        when(mockDelegate.execute(request)).thenReturn(expected);

        // First call: Cache miss -> calls upstream
        UpstreamResponse res1 = client.execute(request);
        assertEquals(200, res1.getStatusCode());
        assertEquals("{\"items\": [1, 2, 3]}", res1.getBody());
        verify(mockDelegate, times(1)).execute(request);

        // Second call: Cache hit -> returns cached response without calling upstream
        UpstreamResponse res2 = client.execute(request);
        assertEquals(200, res2.getStatusCode());
        assertEquals("{\"items\": [1, 2, 3]}", res2.getBody());
        verify(mockDelegate, times(1)).execute(request);
    }

    @Test
    @DisplayName("TTL expiration using fake Clock: response expires after TTL (MAG-FR-03, MAG-QR-10)")
    void testTtlExpiry() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(30), 100);

        GatewayRequest request = GatewayRequest.builder()
                .method("GET")
                .path("/api/weather")
                .build();

        UpstreamResponse resAtT0 = UpstreamResponse.builder()
                .statusCode(200)
                .body("sunny")
                .build();

        UpstreamResponse resAtT1 = UpstreamResponse.builder()
                .statusCode(200)
                .body("rainy")
                .build();

        when(mockDelegate.execute(request)).thenReturn(resAtT0, resAtT1);

        // t = 0: miss
        assertEquals("sunny", client.execute(request).getBody());
        verify(mockDelegate, times(1)).execute(request);

        // t = +15s (before TTL of 30s): hit
        fakeClock.advance(Duration.ofSeconds(15));
        assertEquals("sunny", client.execute(request).getBody());
        verify(mockDelegate, times(1)).execute(request);

        // t = +31s from start (past TTL of 30s): expired -> miss -> calls upstream again
        fakeClock.advance(Duration.ofSeconds(16));
        assertEquals("rainy", client.execute(request).getBody());
        verify(mockDelegate, times(2)).execute(request);
    }

    @Test
    @DisplayName("TTL from Cache-Control max-age header overrides default TTL")
    void testCacheControlMaxAgeTtl() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(300), 100);

        GatewayRequest request = GatewayRequest.builder()
                .method("GET")
                .path("/api/short-lived")
                .build();

        UpstreamResponse response = UpstreamResponse.builder()
                .statusCode(200)
                .header("Cache-Control", "public, max-age=10")
                .body("v1")
                .build();

        when(mockDelegate.execute(request)).thenReturn(response, UpstreamResponse.builder().statusCode(200).body("v2").build());

        assertEquals("v1", client.execute(request).getBody());

        // advance 5s (less than max-age 10s) -> hit
        fakeClock.advance(Duration.ofSeconds(5));
        assertEquals("v1", client.execute(request).getBody());
        verify(mockDelegate, times(1)).execute(request);

        // advance 6s (total 11s > max-age 10s) -> miss
        fakeClock.advance(Duration.ofSeconds(6));
        assertEquals("v2", client.execute(request).getBody());
        verify(mockDelegate, times(2)).execute(request);
    }

    @Test
    @DisplayName("Bounded LRU eviction in CachingUpstreamClient (MAG-QR-09)")
    void testLruEviction() {
        BoundedLruCache<CacheKey, CacheEntry> cache = new BoundedLruCache<>(2);
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, cache, Duration.ofSeconds(60));

        GatewayRequest req1 = GatewayRequest.builder().method("GET").path("/item/1").build();
        GatewayRequest req2 = GatewayRequest.builder().method("GET").path("/item/2").build();
        GatewayRequest req3 = GatewayRequest.builder().method("GET").path("/item/3").build();

        when(mockDelegate.execute(any())).thenAnswer(invocation -> {
            GatewayRequest req = invocation.getArgument(0);
            return UpstreamResponse.builder().statusCode(200).body("body-for-" + req.getPath()).build();
        });

        client.execute(req1); // cache: [req1]
        client.execute(req2); // cache: [req1, req2]

        // Access req1 so req2 becomes LRU
        client.execute(req1); // cache: [req2, req1]

        // Put req3 -> req2 evicted
        client.execute(req3); // cache: [req1, req3]

        // Reset invocation counts to clearly test cache hits
        // req1 should hit
        client.execute(req1);
        // req3 should hit
        client.execute(req3);
        // req2 was evicted, so it must call upstream
        client.execute(req2);

        // In total: req1 called 1 time, req2 called 2 times, req3 called 1 time
        verify(mockDelegate, times(1)).execute(req1);
        verify(mockDelegate, times(2)).execute(req2);
        verify(mockDelegate, times(1)).execute(req3);
    }

    @Test
    @DisplayName("Single-flight: 100 concurrent cache misses for the same key produce exactly 1 upstream call (MAG-QR-01, MAG-QR-06)")
    void testSingleFlight100ConcurrentRequests() throws InterruptedException, ExecutionException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger upstreamCalls = new AtomicInteger(0);

        UpstreamClient slowDelegate = request -> {
            upstreamCalls.incrementAndGet();
            try {
                // Simulate upstream latency so other threads pile in
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return UpstreamResponse.builder()
                    .statusCode(200)
                    .body("shared-result")
                    .build();
        };

        CachingUpstreamClient client = new CachingUpstreamClient(slowDelegate, fakeClock, Duration.ofSeconds(60), 1000);

        GatewayRequest request = GatewayRequest.builder()
                .method("GET")
                .path("/api/heavy-resource")
                .build();

        List<Future<UpstreamResponse>> futures = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return client.execute(request);
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();

        for (Future<UpstreamResponse> f : futures) {
            UpstreamResponse resp = f.get();
            assertEquals(200, resp.getStatusCode());
            assertEquals("shared-result", resp.getBody());
        }

        executor.shutdown();

        assertEquals(1, upstreamCalls.get(), "Single-flight MUST produce exactly 1 upstream call for 100 concurrent requests");
    }

    @Test
    @DisplayName("User-safe cache keys: Different users never share authenticated/private cached responses (MAG-QR-06)")
    void testDifferentUsersNeverShareCachedResponses() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest userARequest = GatewayRequest.builder()
                .method("GET")
                .path("/api/user/profile")
                .header("Authorization", "Bearer user-A-secret-token")
                .build();

        GatewayRequest userBRequest = GatewayRequest.builder()
                .method("GET")
                .path("/api/user/profile")
                .header("Authorization", "Bearer user-B-secret-token")
                .build();

        UpstreamResponse userAResponse = UpstreamResponse.builder()
                .statusCode(200)
                .body("{\"user\": \"Alice\"}")
                .build();

        UpstreamResponse userBResponse = UpstreamResponse.builder()
                .statusCode(200)
                .body("{\"user\": \"Bob\"}")
                .build();

        when(mockDelegate.execute(userARequest)).thenReturn(userAResponse);
        when(mockDelegate.execute(userBRequest)).thenReturn(userBResponse);

        // User A requests profile -> gets Alice
        UpstreamResponse a1 = client.execute(userARequest);
        assertEquals("{\"user\": \"Alice\"}", a1.getBody());

        // User B requests same path -> MUST NOT get Alice from cache; must call upstream and get Bob
        UpstreamResponse b1 = client.execute(userBRequest);
        assertEquals("{\"user\": \"Bob\"}", b1.getBody());

        verify(mockDelegate, times(1)).execute(userARequest);
        verify(mockDelegate, times(1)).execute(userBRequest);

        // Subsequent requests for User A and User B hit their own separate cache entries
        UpstreamResponse a2 = client.execute(userARequest);
        assertEquals("{\"user\": \"Alice\"}", a2.getBody());

        UpstreamResponse b2 = client.execute(userBRequest);
        assertEquals("{\"user\": \"Bob\"}", b2.getBody());

        verify(mockDelegate, times(1)).execute(userARequest);
        verify(mockDelegate, times(1)).execute(userBRequest);
    }

    @Test
    @DisplayName("Non-GET requests are never cached and bypass cache entirely")
    void testNonGetRequestsNeverCached() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest postReq = GatewayRequest.builder()
                .method("POST")
                .path("/api/items")
                .build();

        UpstreamResponse postResp = UpstreamResponse.builder().statusCode(200).body("created").build();
        when(mockDelegate.execute(postReq)).thenReturn(postResp);

        client.execute(postReq);
        client.execute(postReq);

        // POST should never be cached; called every time
        verify(mockDelegate, times(2)).execute(postReq);
    }

    @Test
    @DisplayName("Non-200 responses are never cached")
    void testNon200ResponsesNeverCached() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest req404 = GatewayRequest.builder().method("GET").path("/api/missing").build();
        UpstreamResponse resp404 = UpstreamResponse.builder().statusCode(404).body("Not Found").build();
        when(mockDelegate.execute(req404)).thenReturn(resp404);

        client.execute(req404);
        client.execute(req404);
        verify(mockDelegate, times(2)).execute(req404);

        GatewayRequest req500 = GatewayRequest.builder().method("GET").path("/api/error").build();
        UpstreamResponse resp500 = UpstreamResponse.builder().statusCode(500).body("Error").build();
        when(mockDelegate.execute(req500)).thenReturn(resp500);

        client.execute(req500);
        client.execute(req500);
        verify(mockDelegate, times(2)).execute(req500);
    }

    @Test
    @DisplayName("Responses with Set-Cookie header are excluded from caching")
    void testSetCookieResponsesNeverCached() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest req = GatewayRequest.builder().method("GET").path("/api/login-status").build();
        UpstreamResponse respWithCookie = UpstreamResponse.builder()
                .statusCode(200)
                .header("Set-Cookie", "session=xyz; Path=/")
                .body("ok")
                .build();

        when(mockDelegate.execute(req)).thenReturn(respWithCookie);

        client.execute(req);
        client.execute(req);

        verify(mockDelegate, times(2)).execute(req);
    }

    @Test
    @DisplayName("Responses with Cache-Control: private or no-store are excluded from caching")
    void testPrivateResponsesNeverCached() {
        CachingUpstreamClient client = new CachingUpstreamClient(mockDelegate, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest reqPrivate = GatewayRequest.builder().method("GET").path("/api/personal").build();
        UpstreamResponse respPrivate = UpstreamResponse.builder()
                .statusCode(200)
                .header("Cache-Control", "private, max-age=3600")
                .body("personal-data")
                .build();

        when(mockDelegate.execute(reqPrivate)).thenReturn(respPrivate);

        client.execute(reqPrivate);
        client.execute(reqPrivate);

        verify(mockDelegate, times(2)).execute(reqPrivate);

        GatewayRequest reqNoStore = GatewayRequest.builder().method("GET").path("/api/ephemeral").build();
        UpstreamResponse respNoStore = UpstreamResponse.builder()
                .statusCode(200)
                .header("Cache-Control", "no-store")
                .body("secret")
                .build();

        when(mockDelegate.execute(reqNoStore)).thenReturn(respNoStore);

        client.execute(reqNoStore);
        client.execute(reqNoStore);

        verify(mockDelegate, times(2)).execute(reqNoStore);
    }

    @Test
    @DisplayName("Cache hit bypasses underlying ResilientUpstreamClient / CircuitBreaker")
    void testCacheHitBypassesResilientUpstreamClient() {
        UpstreamClient resilientClientMock = mock(UpstreamClient.class);
        CachingUpstreamClient client = new CachingUpstreamClient(resilientClientMock, fakeClock, Duration.ofSeconds(60), 100);

        GatewayRequest req = GatewayRequest.builder().method("GET").path("/api/fast-data").build();
        UpstreamResponse resp = UpstreamResponse.builder().statusCode(200).body("cached-data").build();

        when(resilientClientMock.execute(req)).thenReturn(resp);

        // 1st request -> misses cache -> reaches resilient upstream client
        UpstreamResponse r1 = client.execute(req);
        assertEquals("cached-data", r1.getBody());
        verify(resilientClientMock, times(1)).execute(req);

        // 2nd, 3rd, 4th request -> hits cache -> resilient client is NEVER called again
        client.execute(req);
        client.execute(req);
        client.execute(req);

        verify(resilientClientMock, times(1)).execute(req);
    }
}
