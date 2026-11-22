package com.mag.core.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSnapshotTest {

    @Test
    void snapshotIsAnImmutableRecordAndCopiesItsRoutes() {
        assertTrue(ConfigSnapshot.class.isRecord());

        List<RouteConfig> mutableRoutes = new ArrayList<>();
        mutableRoutes.add(RouteConfig.builder()
                .id("route-1")
                .pathPattern("/api/**")
                .build());

        ConfigSnapshot snapshot = new ConfigSnapshot(1L, mutableRoutes);
        mutableRoutes.clear();

        assertEquals(1L, snapshot.version());
        assertEquals(1, snapshot.routes().size());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.routes().clear());
    }
}
