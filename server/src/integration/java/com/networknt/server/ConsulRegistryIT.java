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

import com.networknt.cluster.Cluster;
import com.networknt.cluster.LightCluster;
import com.networknt.config.Config;
import com.networknt.consul.ConsulConfig;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.switcher.SwitcherUtil;
import com.networknt.utility.Constants;
import com.networknt.utility.Util;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static com.networknt.consul.ConsulConstants.CONFIG_NAME;
import static com.networknt.server.Server.STATUS_HOST_IP;

/**
 * This test is an automatic test for real consul registry and discovery by using testContainers.
 * Since it depends on docker so it should be disabled all the time unless it is used.
 */
public class ConsulRegistryIT {
    static final Logger logger = LoggerFactory.getLogger(ConsulRegistryIT.class);
    static LightCluster lightCluster;
    static String serviceId = "com.networknt.petstore-1.0.0";
    static String ipAddress;

    static ConsulConfig consulConfig = null;
    static ServerConfig serverConfig = null;

    static Server server1 = null;
    static Server server2 = null;
    static Server server3 = null;

    static final int retryTimes = 5;

    @ClassRule
    public static GenericContainer consul
            = new GenericContainer("consul")
            .withExposedPorts(8500);

    @BeforeClass
    public static void setUp() throws InterruptedException {
        Config.getInstance().clear();
        //Update consul and service config with dynamic port generated by container test
        Map<String, Object> valueConfig = new HashMap<>();
        Integer port = consul.getFirstMappedPort();
        valueConfig.put("container.port", port);
        Config.getInstance().putInConfigCache("values", valueConfig);
        consulConfig = (ConsulConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ConsulConfig.class);
        serverConfig = ServerConfig.getInstance();
        serverConfig.setEnableRegistry(true);
        consulConfig.setConsulUrl("http://localhost:" + port);

        //set ip address
        ipAddress = System.getenv(STATUS_HOST_IP);
        logger.info("Registry IP from STATUS_HOST_IP is " + ipAddress);
        if (ipAddress == null) {
            InetAddress inetAddress = Util.getInetAddress();
            ipAddress = inetAddress.getHostAddress();
            logger.info("Could not find IP from STATUS_HOST_IP, use the InetAddress " + ipAddress);
        }

        //construct cluster
        lightCluster = (LightCluster) SingletonServiceFactory.getBean(Cluster.class);
        //start server without registry, the registry is be done separately for testing
        startServer(49588, null, server1);
        startServer(49589, "", server2);
        startServer(49590, "dev", server3);
        // Wait for server start complete
        Thread.sleep(10000);
    }

    @AfterClass
    public static void tearDown() {
        // Skip the service unregistering since the test container tear down before the server tear down
        serverConfig.setEnableRegistry(false);
        if (server1 != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            Server.stop();
            logger.info("The server1 is stopped.");
        }
        if (server2 != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            server2.stop();
            logger.info("The server2 is stopped.");
        }
        if (server3 != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            server3.stop();
            logger.info("The server3 is stopped.");
        }
    }

    @Test
    public void testDiscoverWithoutEnvTag() throws InterruptedException {
        String url = null;
        for (int i = 0; i < retryTimes; i++) {
            url = lightCluster.serviceToUrl("https", serviceId, null, null);
            if (url != null) break;
            Thread.sleep(2000);
        }
        Assert.assertNotNull(url);
        Assert.assertTrue(url.equals("https://" + ipAddress + ":49588") || url.equals("https://" + ipAddress + ":49589") || url.equals("https://" + ipAddress + ":49590"));
    }

    @Test
    public void testDiscoverWithEmptyStringEnvTag() throws InterruptedException {
        String url = null;
        for (int i = 0; i < retryTimes; i++) {
            url = lightCluster.serviceToUrl("https", serviceId, "", null);
            if (url != null) break;
            Thread.sleep(2000);
        }
        Assert.assertNotNull(url);
        Assert.assertTrue(url.equals("https://" + ipAddress + ":49588") || url.equals("https://" + ipAddress + ":49589"));
    }

    @Test
    public void testDiscoverWithNonEmptyStringEnvTag() throws InterruptedException {
        String url = null;
        for (int i = 0; i < retryTimes; i++) {
            url = lightCluster.serviceToUrl("https", serviceId, "dev", null);
            if (url != null) break;
            Thread.sleep(2000);
        }
        Assert.assertNotNull(url);
        Assert.assertEquals("https://" + ipAddress + ":49590", url);
    }

    @Test
    public void testDiscoverWithNonexistentEnvTag() {
        String url = lightCluster.serviceToUrl("https", serviceId, "sit", null);
        Assert.assertNull(url);
    }

    @Test
    public void testDiscoverCaching() throws InterruptedException {
        String url = null;
        for (int i = 0; i < retryTimes; i++) {
            url = lightCluster.serviceToUrl("https", serviceId, "dev", null);
            if (url != null) break;
            Thread.sleep(2000);
        }
        Assert.assertNotNull(url);
        Assert.assertEquals("https://" + ipAddress + ":49590", url);

        url = lightCluster.serviceToUrl("https", serviceId, "sit", null);
        Assert.assertNull(url);

        url = lightCluster.serviceToUrl("https", serviceId, "dev", null);
        Assert.assertNotNull(url);
        Assert.assertEquals("https://" + ipAddress + ":49590", url);
    }

    private static void startServer(int port, String envTag, Server server) throws InterruptedException {
        SwitcherUtil.setSwitcherValue(Constants.REGISTRY_HEARTBEAT_SWITCHER, false);
        serverConfig.setHttpsPort(port);
        serverConfig.setEnvironment(envTag);
        logger.info("starting server");
        new Thread(() -> server.start()).start();
        // Wait for server start complete
        Thread.sleep(1000);
    }
}
