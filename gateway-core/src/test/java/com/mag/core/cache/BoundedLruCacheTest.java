package com.mag.core.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedLruCacheTest {

    @Test
    @DisplayName("Reject invalid capacity <= 0")
    void testInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedLruCache<String, String>(0));
        assertThrows(IllegalArgumentException.class, () -> new BoundedLruCache<String, String>(-5));
    }

    @Test
    @DisplayName("Basic put, get, size, and containsKey")
    void testPutAndGet() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(3);
        assertEquals(0, cache.size());
        assertEquals(3, cache.getMaxEntries());

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertFalse(cache.containsKey("k3"));

        assertEquals("v1", cache.get("k1"));
        assertEquals("v2", cache.get("k2"));
        assertNull(cache.get("k3"));
    }

    @Test
    @DisplayName("LRU eviction when capacity is exceeded (MAG-QR-09)")
    void testLruEviction() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);

        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertEquals(2, cache.size());

        // Insert 3rd item: eldest (k1) should be evicted
        cache.put("k3", "v3");
        assertEquals(2, cache.size());

        assertNull(cache.get("k1"), "k1 should have been evicted as LRU");
        assertEquals("v2", cache.get("k2"));
        assertEquals("v3", cache.get("k3"));
    }

    @Test
    @DisplayName("Access order: accessing an entry marks it as most recently used")
    void testAccessOrderUpdatesLru() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        // Access k1 so k2 becomes the eldest (LRU)
        assertEquals("v1", cache.get("k1"));

        // Put k3: k2 should be evicted instead of k1
        cache.put("k3", "v3");

        assertEquals("v1", cache.get("k1"), "k1 was recently accessed, should NOT be evicted");
        assertNull(cache.get("k2"), "k2 was LRU and should be evicted");
        assertEquals("v3", cache.get("k3"));
    }

    @Test
    @DisplayName("Remove and clear operations")
    void testRemoveAndClear() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(5);
        cache.put("a", "1");
        cache.put("b", "2");

        assertEquals("1", cache.remove("a"));
        assertNull(cache.get("a"));
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("b"));
    }
}
