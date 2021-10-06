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

package com.networknt.router;

import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class RouterConfigTest {

    private static RouterConfig routerConfig;

    @BeforeClass
    public static void setUp() {
        routerConfig = RouterConfig.load();
    }


    @Test
    public void testConfig() {
        Assert.assertFalse(routerConfig.isHttp2Enabled());
        Assert.assertTrue(routerConfig.isHttpsEnabled());
        Assert.assertTrue(routerConfig.isRewriteHostHeader());
        Assert.assertEquals(routerConfig.getMaxRequestTime(), 1000);
        Assert.assertEquals(routerConfig.getMaxConnectionRetries(), 3);
    }

    @Test
    public void testConfigList() {
        Assert.assertNotNull(routerConfig.getHostWhitelist());
        Assert.assertEquals(routerConfig.getHostWhitelist().size(), 2);
    }

}
