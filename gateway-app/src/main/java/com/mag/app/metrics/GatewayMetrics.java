package com.mag.app.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Live metrics tracking for the MAG Demo Dashboard (Panel 1).
 * Thread-safe: uses AtomicLong counters.
 */
@Component
public class GatewayMetrics {

    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong blockedRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    public void recordAllowed() {
        allowedRequests.incrementAndGet();
    }

    public void recordBlocked() {
        blockedRequests.incrementAndGet();
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
        allowedRequests.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public long getAllowedRequests() {
        return allowedRequests.get();
    }

    public long getBlockedRequests() {
        return blockedRequests.get();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public String getCacheHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        if (total == 0) {
            return "0.0%";
        }
        double ratio = (hits * 100.0) / total;
        return String.format("%.1f%%", ratio);
    }
}
