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
package com.networknt.portal.registry;

import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Hu
 */
public class PortalRegistryUtilsTest {
    String testHost;
    String testProtocol;
    int testPort;
    URL url;


    String testServiceId;
    String testServiceTag;

    @Before
    public void setUp() throws Exception {
        testServiceId = "com.networknt.apia-1.0.0";
        testServiceTag = "uat1";
        testHost = "127.0.0.1";
        testPort = 8888;
        testProtocol = "light";
        url = new URLImpl(testProtocol, testHost, testPort, testServiceId);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConvertServiceId() {
        String tempServiceId = PortalRegistryUtils.convertPortalRegistrySerivceId(url);
        assertEquals(testServiceId, tempServiceId);
    }
}
