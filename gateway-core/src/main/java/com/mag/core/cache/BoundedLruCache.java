package com.mag.core.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * M-C: Fast-Path Cache — Bounded LRU Cache.
 *
 * Responsibilities &amp; Thread-Safety:
 *   - Guarded by a {@link ReentrantLock} to ensure thread safety across concurrent readers and writers.
 *   - Backed by an access-order {@link LinkedHashMap} which automatically evicts the least recently
 *     used entry when the entry count exceeds {@code maxEntries} (MAG-QR-09).
 *   - In access-order mode, even {@link #get(Object)} modifies node pointers, so mutual exclusion
 *     is strictly required for both reads and writes.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class BoundedLruCache<K, V> {

    private final int maxEntries;
    private final Map<K, V> map;
    private final ReentrantLock lock = new ReentrantLock();

    public BoundedLruCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0, got: " + maxEntries);
        }
        this.maxEntries = maxEntries;
        this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public V get(K key) {
        Objects.requireNonNull(key, "key");
        lock.lock();
        try {
            return map.get(key);
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        lock.lock();
        try {
            map.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public V remove(K key) {
        Objects.requireNonNull(key, "key");
        lock.lock();
        try {
            return map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key");
        lock.lock();
        try {
            return map.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void clear() {
        lock.lock();
        try {
            map.clear();
        } finally {
            lock.unlock();
        }
    }
}
