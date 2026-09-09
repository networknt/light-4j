package com.networknt.server;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatusConfigCacheTest {
    @TempDir Path directory;

    @Test
    void applicationStatusCodesSurviveOtherRepresentations() throws Exception {
        Config config = Config.getInstance();
        Files.createDirectories(directory.resolve("config"));
        Files.writeString(directory.resolve("config/app-status.yml"), "CACHE_TEST_STATUS:\n  statusCode: 400\n  code: CACHE_TEST_STATUS\n  message: cache regression\n");
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, getClass().getClassLoader())) {
            config.setClassLoader(loader);
            for (boolean typedFirst : new boolean[]{true, false}) {
                config.clearConfigCache("status");
                if (typedFirst) config.getJsonObjectConfig("status", HashMap.class);
                Map<String, Object> map = config.getJsonMapConfig("status");
                Server.mergeStatusConfig();
                assertNotNull(map.get("CACHE_TEST_STATUS"));
                config.getJsonObjectConfig("status", HashMap.class);
                assertSame(map, config.getJsonMapConfig("status"));
                assertNotNull(config.getJsonMapConfig("status").get("CACHE_TEST_STATUS"));
            }
        } finally {
            config.setClassLoader(null);
        }
    }
}
