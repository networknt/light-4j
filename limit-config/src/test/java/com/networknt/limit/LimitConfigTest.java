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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.List;
import java.util.Map;



public class LimitConfigTest {

    private static LimitConfig limitConfig;

    @BeforeEach
    public void setUp() {
        limitConfig = LimitConfig.load();
    }

    @Test
    public void testConfigData() {
        Assertions.assertTrue(limitConfig.isEnabled());
        Assertions.assertEquals(limitConfig.getConcurrentRequest(), 1);
        Assertions.assertEquals(limitConfig.getQueueSize(), 1);
        Assertions.assertEquals(limitConfig.getErrorCode(), 429);
    }

    @Test
    public void testLimitKey() {
        Assertions.assertEquals(limitConfig.getKey(), LimitKey.SERVER);
    }

    @Test
    public void testRateLimit() {
        List<LimitQuota> limitQuotaList =  limitConfig.getRateLimit();
        Assertions.assertEquals(limitQuotaList.size(), 2);
    }

    @Test
    public void testServer() {
        Map<String, LimitQuota> limitServer =  limitConfig.getServer();
        Assertions.assertEquals(limitServer.size(), 2);
    }

    @Test
    public void testAddress() {
        RateLimitSet limitAddress =  limitConfig.getAddress();
        Assertions.assertEquals(limitAddress.getDirectMaps().size(), 4);
    }

    @Test
    public void testClient() {
        RateLimitSet limitClient =  limitConfig.getClient();
        Assertions.assertEquals(limitClient.getDirectMaps().size(), 4);

    }

    @Test
    public void testUser() {
        RateLimitSet limitUser =  limitConfig.getUser();
        Assertions.assertEquals(limitUser.getDirectMaps().size(), 3);

    }

}
