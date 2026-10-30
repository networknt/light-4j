/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.server;

import com.networknt.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerOptionRegistryTest {
    private static final String KEY = "server:" + ServerConfig.class.getName();

    @AfterEach
    void clearConfig() {
        Config.getInstance().clear();
        ServerConfig.load();
    }

    @Test
    void invalidOptionsAreReportedAsValidatedValues() {
        Config.getInstance().clear();
        Map<String, Object> source = Config.getInstance().getJsonMapConfig("server");
        for (String name : List.of("ioThreads", "workerThreads", "bufferSize", "backlog",
                "maxTransferFileSize", "shutdownTimeout")) {
            source.put(name, -1);
        }
        source.put("serverString", "");
        source.put("maskConfigProperties", true);
        ServerConfig server = ServerConfig.load();
        // Seed the same raw values that registration receives from a negative-valued server.yml.
        ModuleRegistry.registerModule("server", ServerConfig.class.getName(),
                new HashMap<>(source), List.of("keystorePass", "keyPass", "truststorePass", "bootstrapStorePass"));
        Map<String, Object> before = reported();
        assertEquals(-1, before.get("maxTransferFileSize"));

        ServerOption.serverOptionInit(source, server);

        assertReportedOptions(server);
        assertEquals(1_000_000L, reported().get("maxTransferFileSize"));
        assertNull(reported().get("shutdownTimeout"));
        assertEquals(-1, source.get("maxTransferFileSize"));
        assertEquals(-1, before.get("maxTransferFileSize"));
        for (String secret : List.of("keystorePass", "keyPass", "truststorePass", "bootstrapStorePass")) {
            assertNotEquals(source.get(secret), reported().get(secret));
        }
        // Publishing a new snapshot must also work on subsequent initialization.
        ServerOption.serverOptionInit(source, server);
        assertReportedOptions(server);
    }

    @Test
    void absentOptionsAreReportedWithEffectiveDefaults() {
        Config.getInstance().clear();
        ServerConfig server = ServerConfig.load();
        for (ServerOption option : ServerOption.values()) {
            server.getMappedConfig().remove(option.value());
        }
        ServerOption.serverOptionInit(server.getMappedConfig(), server);
        assertReportedOptions(server);
        assertEquals(1_000_000L, reported().get("maxTransferFileSize"));
    }

    @Test
    void validOptionsAreReportedAndNamedConfigurationsDoNotReplaceRunningServer() {
        Config.getInstance().clear();
        Map<String, Object> source = Config.getInstance().getJsonMapConfig("server");
        source.put("maxTransferFileSize", 1234567L);
        source.put("shutdownTimeout", 1234);
        ServerConfig server = ServerConfig.load();
        ServerOption.serverOptionInit(source, server);
        assertReportedOptions(server);
        assertEquals(1234567L, reported().get("maxTransferFileSize"));
        assertEquals(1234, reported().get("shutdownTimeout"));
        Map<String, Object> nonDecrypted = Config.getNoneDecryptedInstance().getJsonMapConfigNoCache("server");
        for (String secret : List.of("keystorePass", "keyPass", "truststorePass", "bootstrapStorePass")) {
            assertEquals(nonDecrypted.get(secret), reported().get(secret));
        }
        Map<String, Object> snapshot = reported();
        ServerConfig named = ServerConfig.load("server_invalid_option");
        ServerOption.serverOptionInit(named.getMappedConfig(), named);
        assertSame(snapshot, reported());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> reported() {
        return (Map<String, Object>) ModuleRegistry.getModuleRegistry().get(KEY);
    }

    private static void assertReportedOptions(ServerConfig server) {
        Map<String, Object> config = reported();
        assertEquals(server.getIoThreads(), config.get("ioThreads"));
        assertEquals(server.getWorkerThreads(), config.get("workerThreads"));
        assertEquals(server.getBufferSize(), config.get("bufferSize"));
        assertEquals(server.getBacklog(), config.get("backlog"));
        assertEquals(server.getServerString(), config.get("serverString"));
        assertEquals(server.isAlwaysSetDate(), config.get("alwaysSetDate"));
        assertEquals(server.getMaxTransferFileSize(), config.get("maxTransferFileSize"));
        assertEquals(server.isAllowUnescapedCharactersInUrl(), config.get("allowUnescapedCharactersInUrl"));
        assertEquals(server.getShutdownTimeout(), config.get("shutdownTimeout"));
    }
}
