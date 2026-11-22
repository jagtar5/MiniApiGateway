package com.mag.core.config;

import com.mag.core.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteConfigTest {

    @Test
    void rejectsMissingOrBlankRequiredRouteFieldsAsConfigurationErrors() {
        assertThrows(InvalidConfigurationException.class,
                () -> RouteConfig.builder().pathPattern("/api/**").build());
        assertThrows(InvalidConfigurationException.class,
                () -> RouteConfig.builder().id("route").pathPattern(" ").build());
    }

    @Test
    void rejectsMalformedRouteCollectionsAndNegativeTtlAsConfigurationErrors() {
        assertThrows(InvalidConfigurationException.class,
                () -> RouteConfig.builder().id("route").pathPattern("/api/**").upstreamUrls(null).build());
        assertThrows(InvalidConfigurationException.class,
                () -> RouteConfig.builder().id("route").pathPattern("/api/**")
                        .methods(Arrays.asList("GET", null)).build());
        assertThrows(InvalidConfigurationException.class,
                () -> RouteConfig.builder().id("route").pathPattern("/api/**").cacheTtlSeconds(-1).build());
    }
}
