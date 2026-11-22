package com.mag.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConfigManagerTest {

    private static ConfigManager newManager() {
        return new ConfigManager(new ConfigValidator(() -> Set.of("token-bucket", "fixed-window", "sliding-window")));
    }

    private static ConfigSnapshot configWith(String... routeIds) {
        List<RouteConfig> routes = new ArrayList<>();
        for (String id : routeIds) {
            routes.add(ConfigValidatorTest.route(id));
        }
        return new ConfigSnapshot(0, routes, List.of());
    }

    /** Immutability: records have no setters, and their lists reject mutation at runtime. */
    @Test
    void snapshotIsImmutable() {
        ConfigSnapshot s = configWith("a");
        assertThrows(UnsupportedOperationException.class, () -> s.routes().add(ConfigValidatorTest.route("b")));
        assertThrows(UnsupportedOperationException.class, () -> s.rules().clear());
        // defensive copy: changing the source list afterwards does not change the snapshot
        List<RouteConfig> source = new ArrayList<>(List.of(ConfigValidatorTest.route("x")));
        ConfigSnapshot copy = new ConfigSnapshot(0, source, List.of());
        source.clear();
        assertEquals(1, copy.routes().size());
    }

    @Test
    void reloadSwapsSnapshotAndIncrementsVersion() {
        ConfigManager m = newManager();
        assertEquals(0, m.current().version());
        m.reload(configWith("a"));
        assertEquals(1, m.current().version());
        m.reload(configWith("a", "b"));
        assertEquals(2, m.current().version());
        assertEquals(2, m.current().routes().size());
    }

    @Test
    void lastKnownGoodIsKeptWhenNewConfigIsInvalid() {
        ConfigManager m = newManager();
        m.reload(configWith("a"));
        ConfigSnapshot good = m.current();

        ConfigSnapshot invalid = new ConfigSnapshot(0,
                List.of(new RouteConfig("a", "/a", "http://h/", -5, 1000)), List.of());
        assertThrows(ConfigValidationException.class, () -> m.reload(invalid));

        assertSame(good, m.current());   // exact same object still served
    }

    @Test
    void listenersAreCalledAfterSuccessfulSwapOnly() {
        ConfigManager m = newManager();
        AtomicInteger calls = new AtomicInteger();
        m.addListener(s -> calls.incrementAndGet());
        m.reload(configWith("a"));
        assertThrows(ConfigValidationException.class,
                () -> m.reload(new ConfigSnapshot(0, List.of(new RouteConfig(null, null, null, 0, 0)), List.of())));
        assertEquals(1, calls.get());
    }

    @Test
    void aFailingListenerDoesNotUndoTheSwap() {
        ConfigManager m = newManager();
        m.addListener(s -> { throw new IllegalStateException("boom"); });
        m.reload(configWith("a"));
        assertEquals(1, m.current().version());
    }

    @Test
    void reloadFromStoreAppliesPersistedConfig() {
        InMemoryConfigStore store = new InMemoryConfigStore();
        store.save(configWith("persisted"));
        ConfigManager m = newManager();
        m.reloadFrom(store);
        assertEquals("persisted", m.current().routes().get(0).id());
    }

    /**
     * Hot reload under load: 20 readers spin while the config is swapped 200 times.
     * Every read must return a complete, valid snapshot (never null, never half-built).
     */
    @Test
    void hotReloadUnderConcurrentReadersDropsNoReads() throws Exception {
        ConfigManager m = newManager();
        m.reload(configWith("a"));
        int readers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger bad = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();
        java.util.concurrent.atomic.AtomicBoolean stop = new java.util.concurrent.atomic.AtomicBoolean(false);

        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < readers; i++) {
            fs.add(pool.submit(() -> {
                start.await();
                while (!stop.get()) {
                    ConfigSnapshot s = m.current();
                    reads.incrementAndGet();
                    if (s == null || s.routes().isEmpty() || s.routes().size() > 2) {
                        bad.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        Future<?> writer = pool.submit(() -> {
            start.await();
            for (int i = 0; i < 200; i++) {
                m.reload(i % 2 == 0 ? configWith("a", "b") : configWith("a"));
            }
            return null;
        });
        start.countDown();
        writer.get(30, TimeUnit.SECONDS);
        stop.set(true);
        for (Future<?> f : fs) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertEquals(0, bad.get(), "reads that saw a null/half-built snapshot");
        assertTrue(reads.get() > 0);
        assertEquals(201, m.current().version());
    }
}
