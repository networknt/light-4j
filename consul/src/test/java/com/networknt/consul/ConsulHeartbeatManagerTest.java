/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.consul;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @Description ConsulHeartbeatManagerTest
 * @author zhanglei28
 *
 */
public class ConsulHeartbeatManagerTest {
    private ConsulHeartbeatManager heartbeatManager;
    private MockConsulClient client;
    private String token;

    @Before
    public void setUp() throws Exception {
        client = new MockConsulClient("localhost", 8500);
        heartbeatManager = new ConsulHeartbeatManager(client, null);

        ConsulConstants.HEARTBEAT_CIRCLE = 200;
        ConsulConstants.SWITCHER_CHECK_CIRCLE = 20;
    }

    @After
    public void tearDown() throws Exception {
        heartbeatManager = null;
    }

    @Test
    public void testStart() throws InterruptedException {
        heartbeatManager.start();
        Map<String, Long> mockServices = new HashMap<String, Long>();
        int serviceNum = 5;

        for (int i = 0; i < serviceNum; i++) {
            String serviceid = "service" + i;
            mockServices.put(serviceid, 0L);
            heartbeatManager.addHeartbeatServcieId(serviceid);
        }

        // switch on heart beat
        setHeartbeatSwitcher(true);
        checkHeartbeat(mockServices, true, serviceNum);

        // switch off heart beat
        setHeartbeatSwitcher(false);
        Thread.sleep(100);
        checkHeartbeat(mockServices, false, serviceNum);

    }

    private void checkHeartbeat(Map<String, Long> services, boolean start, int times) throws InterruptedException {
        // check heart beats
        for (int i = 0; i < times; i++) {
            Thread.sleep(ConsulConstants.HEARTBEAT_CIRCLE + 500);
            for (Entry<String, Long> entry : services.entrySet()) {
                long heartbeatTimes = client.getCheckPassTimes(entry.getKey());
                long lastHeartbeatTimes = services.get(entry.getKey());
                services.put(entry.getKey(), heartbeatTimes);
                if (start) { // heart beat open, increase heart beat number
                    assertTrue(heartbeatTimes > lastHeartbeatTimes);
                } else {// heart beat closed, heart beat number is unchanged
                    assertTrue(heartbeatTimes == lastHeartbeatTimes);
                }
            }
        }
    }

    public void setHeartbeatSwitcher(boolean value) {
        heartbeatManager.setHeartbeatOpen(value);

    }

}
