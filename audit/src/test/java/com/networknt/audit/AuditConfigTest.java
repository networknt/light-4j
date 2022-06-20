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
package com.networknt.audit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AuditConfigTest {

    @Before
    public void setUp() {
    }

    @Test
    public void shouldLoadEmptyConfig() {
        AuditConfig config = AuditConfig.load();
        Assert.assertTrue(config.hasAuditList());
        Assert.assertTrue(config.hasHeaderList());
        Assert.assertNotNull(config.getAuditFunc());
        Assert.assertEquals(3, config.getHeaderList().size());
        Assert.assertEquals(11, config.getAuditList().size());
        Assert.assertTrue(config.isStatusCode());
        Assert.assertTrue(config.isResponseTime());
        Assert.assertFalse(config.isAuditOnError());
        Assert.assertFalse(config.isMaskEnabled());
        Assert.assertNotNull(config.getTimestampFormat());
    }

    @Test
    public void shouldGetTimestampFormatAndAuditConfig() {
        HashMap<String, Object> configMap = new HashMap<>();
        String format= "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        configMap.put("timestampFormat", format);
        AuditConfig configHandler = AuditConfig.load();
        Assert.assertEquals(format, configHandler.getTimestampFormat());
    }

    @Test
    @Ignore
    public void shouldLoadHeaderInJson() {
        AuditConfig config = AuditConfig.load("audit-json");
        Assert.assertTrue(config.hasAuditList());
        Assert.assertEquals(10, config.getAuditList().size());
    }

}
