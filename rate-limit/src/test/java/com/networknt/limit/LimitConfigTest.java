/*
 * Copyright (c) 2019 Network New Technologies Inc.
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
package com.networknt.limit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import java.util.List;
import java.util.Map;



public class LimitConfigTest {

    private static LimitConfig limitConfig;

    @Before
    public void setUp() {
        limitConfig = LimitConfig.load();
    }

    @Test
    public void testConfigData() {
        Assert.assertTrue(limitConfig.isEnabled());
        Assert.assertEquals(limitConfig.getConcurrentRequest(), 1);
        Assert.assertEquals(limitConfig.getQueueSize(), 1);
        Assert.assertEquals(limitConfig.getErrorCode(), 429);
    }

    @Test
    public void testLimitKey() {
        Assert.assertEquals(limitConfig.getKey(), LimitKey.SERVER);
    }

    @Test
    public void testServer() {
        Map<String, LimitQuota> limitServer =  limitConfig.getServer();
        Assert.assertEquals(limitServer.size(), 2);
    }

    @Test
    public void testAddress() {
        LimitConfig.RateLimitSet limitAddress =  limitConfig.getAddress();
        Assert.assertEquals(limitAddress.getDirectMaps().size(), 2);
        Assert.assertEquals(limitAddress.getPathMaps().size(), 1);

    }

    @Test
    public void testClient() {
        LimitConfig.RateLimitSet limitClient =  limitConfig.getClient();
        Assert.assertEquals(limitClient.getDirectMaps().size(), 2);
        Assert.assertEquals(limitClient.getPathMaps().size(), 1);

    }

}
