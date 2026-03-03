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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerConfigTest {

    public static final String CONFIG_NAME = "server";
    public static final String CONFIG_NAME_TEST = "server_test";

    public static Config config = Config.getInstance();

    @Test
    public void testNullEnv() {
        config.clear();
        // ensure that env is null if it is missing in the server.yml
        ServerConfig serverConfig = ServerConfig.load("server");
        Assertions.assertNull(serverConfig.getEnvironment());
        Assertions.assertEquals("petstore", serverConfig.getServiceName());
    }

    @Test
    public void testDefaultServerOptions() {
        config.clear();
        ServerConfig serverConfig = ServerConfig.load("server");
        ServerOption.serverOptionInit(serverConfig.getMappedConfig(),serverConfig);
        Assertions.assertEquals(1024*16, serverConfig.getBufferSize());
        Assertions.assertEquals(Runtime.getRuntime().availableProcessors() * 2, serverConfig.getIoThreads());
        Assertions.assertEquals(200, serverConfig.getWorkerThreads());
        Assertions.assertEquals(10000, serverConfig.getBacklog());
        Assertions.assertTrue(serverConfig.isAlwaysSetDate());
        Assertions.assertEquals("L", serverConfig.getServerString());
    }

    @Test
    public void testInvalidServerOptions() {
        config.clear();
        ServerConfig serverConfig = ServerConfig.load("server_invalid_option");
        ServerOption.serverOptionInit(serverConfig.getMappedConfig(),serverConfig);
        Assertions.assertEquals(1024*16, serverConfig.getBufferSize());
        Assertions.assertEquals(Runtime.getRuntime().availableProcessors() * 2, serverConfig.getIoThreads());
        Assertions.assertEquals(200, serverConfig.getWorkerThreads());
        Assertions.assertEquals(10000, serverConfig.getBacklog());
        Assertions.assertEquals(false, serverConfig.isAlwaysSetDate());
        Assertions.assertEquals("L", serverConfig.getServerString());
        Assertions.assertEquals(false, serverConfig.isAllowUnescapedCharactersInUrl());
    }

    @Test
    public void testValidServerOptions() {
        config.clear();
        ServerConfig serverConfig = ServerConfig.load("server_valid_option");
        ServerOption.serverOptionInit(serverConfig.getMappedConfig(),serverConfig);
        Assertions.assertEquals(10000, serverConfig.getBufferSize());
        Assertions.assertEquals(1, serverConfig.getIoThreads());
        Assertions.assertEquals(100, serverConfig.getWorkerThreads());
        Assertions.assertEquals(10000, serverConfig.getBacklog());
        Assertions.assertEquals(false, serverConfig.isAlwaysSetDate());
        Assertions.assertEquals("TEST", serverConfig.getServerString());
        Assertions.assertEquals(true, serverConfig.isAllowUnescapedCharactersInUrl());
    }

    @Test
    public void testMaxTransferFileSize() {
        config.clear();
        ServerConfig serverConfig = ServerConfig.load();
        ServerOption.serverOptionInit(serverConfig.getMappedConfig(), serverConfig);
        Assertions.assertEquals(1000000, serverConfig.getMaxTransferFileSize());
    }
}
