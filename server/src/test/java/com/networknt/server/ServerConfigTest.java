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
import org.junit.Assert;
import org.junit.Test;

public class ServerConfigTest {

    public static final String CONFIG_NAME = "server";
    public static final String CONFIG_NAME_TEST = "server_test";

    public static Config config = Config.getInstance();

    @Test
    public void testNullEnv() {
        config.clear();
        // ensure that env is null if it is missing in the server.yml
        ServerConfig serverConfig = (ServerConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServerConfig.class);
        Assert.assertNull(serverConfig.getEnvironment());
        Assert.assertEquals("petstore", serverConfig.getServiceName());
    }

    @Test
    public void testDefaultServerOptions() {
        config.clear();
        ServerConfig serverConfig = (ServerConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServerConfig.class);
        ServerOption.serverOptionInit(config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME),serverConfig);
        Assert.assertEquals(1024*16, serverConfig.getBufferSize());
        Assert.assertEquals(Runtime.getRuntime().availableProcessors() * 2, serverConfig.getIoThreads());
        Assert.assertEquals(200, serverConfig.getWorkerThreads());
        Assert.assertEquals(10000, serverConfig.getBacklog());
        Assert.assertEquals(true, serverConfig.isAlwaysSetDate());
        Assert.assertEquals("L", serverConfig.getServerString());
    }

    @Test
    public void testInvalidServerOptions() {
        config.clear();
        ServerConfig serverConfig = (ServerConfig)Config.getInstance().getJsonObjectConfig("server_invalid_option", ServerConfig.class);
        ServerOption.serverOptionInit(config.getInstance().getJsonMapConfigNoCache("server_invalid_option"), serverConfig);
        Assert.assertEquals(1024*16, serverConfig.getBufferSize());
        Assert.assertEquals(Runtime.getRuntime().availableProcessors() * 2, serverConfig.getIoThreads());
        Assert.assertEquals(200, serverConfig.getWorkerThreads());
        Assert.assertEquals(10000, serverConfig.getBacklog());
        Assert.assertEquals(false, serverConfig.isAlwaysSetDate());
        Assert.assertEquals("L", serverConfig.getServerString());
        Assert.assertEquals(false, serverConfig.isAllowUnescapedCharactersInUrl());
    }

    @Test
    public void testValidServerOptions() {
        config.clear();
        ServerConfig serverConfig = (ServerConfig)Config.getInstance().getJsonObjectConfig("server_valid_option", ServerConfig.class);
        ServerOption.serverOptionInit(config.getInstance().getJsonMapConfigNoCache("server_valid_option"), serverConfig);
        Assert.assertEquals(10000, serverConfig.getBufferSize());
        Assert.assertEquals(1, serverConfig.getIoThreads());
        Assert.assertEquals(100, serverConfig.getWorkerThreads());
        Assert.assertEquals(10000, serverConfig.getBacklog());
        Assert.assertEquals(false, serverConfig.isAlwaysSetDate());
        Assert.assertEquals("TEST", serverConfig.getServerString());
        Assert.assertEquals(true, serverConfig.isAllowUnescapedCharactersInUrl());
    }
}
