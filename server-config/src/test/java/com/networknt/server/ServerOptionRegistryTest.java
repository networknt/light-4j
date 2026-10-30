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
import org.junit.jupiter.api.util.SetSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SetSystemProperty(key = "light_4j_config_password", value = "light")
class ServerOptionRegistryTest {
    private static final String KEY = "server:" + ServerConfig.class.getName();
    private static final String ENCRYPTED = "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5";
    private FixtureLoader decrypted;
    private FixtureLoader raw;

    @AfterEach
    void clearConfig() {
        Config.getInstance().setClassLoader(null);
        Config.getNoneDecryptedInstance().setClassLoader(null);
        Config.getInstance().clear();
        Config.getNoneDecryptedInstance().clear();
        ServerConfig.load();
    }

    @Test
    void invalidOptionsAreValidatedBeforeRegistrationAndRemainCorrectAfterReload() {
        useFixture("""
                ioThreads: -1
                workerThreads: -1
                bufferSize: -1
                backlog: -1
                serverString: ''
                alwaysSetDate: null
                maxTransferFileSize: -1
                allowUnescapedCharactersInUrl: null
                shutdownTimeout: -1
                """, true);
        ServerConfig first = ServerConfig.load();
        assertCorrectedOptions(first);
        assertEquals(-1, first.getMappedConfig().get("maxTransferFileSize"));
        assertEquals("password", first.getKeystorePass());
        assertNotEquals("password", reported().get("keystorePass"));
        assertNotEquals(ENCRYPTED, reported().get("keystorePass"));
        assertEquals(1, decrypted.reads);
        assertEquals(1, raw.reads);
        Map<String, Object> snapshot = reported();
        assertSame(first, ServerConfig.load());
        assertSame(snapshot, reported());
        assertEquals(1, raw.reads);

        // Exercise the production cache-clear/reload path without a server bind or manual registration.
        Config.getInstance().clearConfigCache("server");
        Config.getNoneDecryptedInstance().clearConfigCache("server");
        ServerConfig reloaded = ServerConfig.load();
        assertNotSame(first, reloaded);
        assertNotSame(snapshot, reported());
        assertCorrectedOptions(reloaded);
        assertEquals(2, decrypted.reads);
        assertEquals(2, raw.reads);
    }

    @Test
    void missingOptionsKeepTheRawKeySetWhileRuntimeGetsDefaults() {
        useFixture("", false);
        ServerConfig server = ServerConfig.load();
        assertEquals(1_000_000L, server.getMaxTransferFileSize());
        assertTrue(server.getIoThreads() > 0);
        assertNull(server.getShutdownTimeout());
        assertEquals(server.getMappedConfig().keySet(), reported().keySet());
        for (ServerOption option : ServerOption.values()) {
            assertFalse(reported().containsKey(option.value()));
        }
    }

    @Test
    void validRawRepresentationsAndEncryptedSecretsArePreservedWithMaskingDisabled() {
        useFixture("""
                ioThreads: '2'
                maxTransferFileSize: '1234567'
                shutdownTimeout: '1234'
                """, false);
        ServerConfig server = ServerConfig.load();
        assertEquals(1234567L, server.getMaxTransferFileSize());
        assertEquals(1234, server.getShutdownTimeout());
        assertEquals("2", reported().get("ioThreads"));
        assertEquals("1234567", reported().get("maxTransferFileSize"));
        assertEquals("1234", reported().get("shutdownTimeout"));
        assertEquals(server.getMappedConfig().keySet(), reported().keySet());
        assertEquals("password", server.getKeystorePass());
        assertEquals(ENCRYPTED, reported().get("keystorePass"));
        assertEquals(ENCRYPTED, reported().get("keyPass"));
        assertEquals(ENCRYPTED, reported().get("truststorePass"));
        assertEquals(ENCRYPTED, reported().get("bootstrapStorePass"));
        assertEquals(1, raw.reads);
    }

    @Test
    void missingNonDecryptedConfigDoesNotThrowOrExposeDecryptedValues() {
        useFixture("maxTransferFileSize: -1\n", false);
        raw.yaml = null;
        ServerConfig server = ServerConfig.load();
        assertEquals(1_000_000L, server.getMaxTransferFileSize());
        assertTrue(reported().isEmpty());
    }

    @Test
    void missingConfigStillInitializesRuntimeDefaults() {
        useFixture("", false);
        decrypted.yaml = null;
        raw.yaml = null;
        ServerConfig server = ServerConfig.load();
        assertNull(server.getMappedConfig());
        assertEquals(1_000_000L, server.getMaxTransferFileSize());
        assertTrue(server.getIoThreads() > 0);
        assertTrue(reported().isEmpty());
    }

    @Test
    void namedAndStaleInstancesCannotOverwriteTheRunningServerEntry() {
        useFixture("maxTransferFileSize: -1\n", false);
        ServerConfig stale = ServerConfig.load();
        Config.getInstance().clearConfigCache("server");
        ServerConfig current = ServerConfig.load();
        Map<String, Object> snapshot = reported();
        ServerOption.serverOptionInit(stale.getMappedConfig(), stale);
        ServerConfig named = ServerConfig.load("server_invalid_option");
        assertTrue(named.getIoThreads() > 0);
        assertSame(snapshot, reported());
        assertSame(current, ServerConfig.load());
    }

    private void useFixture(String options, boolean mask) {
        String yaml = options + "maskConfigProperties: " + mask + "\n"
                + "keystorePass: " + ENCRYPTED + "\n"
                + "keyPass: " + ENCRYPTED + "\n"
                + "truststorePass: " + ENCRYPTED + "\n"
                + "bootstrapStorePass: " + ENCRYPTED + "\n";
        decrypted = new FixtureLoader(yaml);
        raw = new FixtureLoader(yaml);
        Config.getInstance().clear();
        Config.getNoneDecryptedInstance().clear();
        Config.getInstance().setClassLoader(decrypted);
        Config.getNoneDecryptedInstance().setClassLoader(raw);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> reported() {
        return (Map<String, Object>) ModuleRegistry.getModuleRegistry().get(KEY);
    }

    private static void assertCorrectedOptions(ServerConfig server) {
        Map<String, Object> config = reported();
        assertEquals(Runtime.getRuntime().availableProcessors() * 2, server.getIoThreads());
        assertEquals(200, server.getWorkerThreads());
        assertEquals(16384, server.getBufferSize());
        assertEquals(10000, server.getBacklog());
        assertEquals("L", server.getServerString());
        assertTrue(server.isAlwaysSetDate());
        assertEquals(1_000_000L, server.getMaxTransferFileSize());
        assertFalse(server.isAllowUnescapedCharactersInUrl());
        assertNull(server.getShutdownTimeout());
        assertEquals(server.getIoThreads(), config.get("ioThreads"));
        assertEquals(server.getWorkerThreads(), config.get("workerThreads"));
        assertEquals(server.getBufferSize(), config.get("bufferSize"));
        assertEquals(server.getBacklog(), config.get("backlog"));
        assertEquals(server.getServerString(), config.get("serverString"));
        assertEquals(server.isAlwaysSetDate(), config.get("alwaysSetDate"));
        assertEquals(server.getMaxTransferFileSize(), config.get("maxTransferFileSize"));
        assertEquals(server.isAllowUnescapedCharactersInUrl(), config.get("allowUnescapedCharactersInUrl"));
        assertTrue(config.containsKey("shutdownTimeout"));
        assertNull(config.get("shutdownTimeout"));
    }

    private static class FixtureLoader extends ClassLoader {
        private String yaml;
        private int reads;

        FixtureLoader(String yaml) {
            super(ServerOptionRegistryTest.class.getClassLoader());
            this.yaml = yaml;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name.matches("(?:config/)?server\\.(?:yml|yaml|json)")) {
                if (name.equals("server.yml") && yaml != null) {
                    reads++;
                    return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
            return super.getResourceAsStream(name);
        }
    }
}
