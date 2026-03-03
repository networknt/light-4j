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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class AuditConfigTest {

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void shouldLoadEmptyConfig() {
        AuditConfig config = AuditConfig.load();
        Assertions.assertTrue(config.hasAuditList());
        Assertions.assertTrue(config.hasHeaderList());
        Assertions.assertNotNull(config.getAuditFunc());
        Assertions.assertEquals(3, config.getHeaderList().size());
        Assertions.assertEquals(11, config.getAuditList().size());
        Assertions.assertTrue(config.isStatusCode());
        Assertions.assertTrue(config.isResponseTime());
        Assertions.assertFalse(config.isAuditOnError());
        Assertions.assertFalse(config.isMask());
        Assertions.assertNotNull(config.getTimestampFormat());
    }

    @Test
    public void shouldGetTimestampFormatAndAuditConfig() {
        HashMap<String, Object> configMap = new HashMap<>();
        String format= "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        configMap.put("timestampFormat", format);
        AuditConfig configHandler = AuditConfig.load();
        Assertions.assertEquals(format, configHandler.getTimestampFormat());
    }

    @Test
    @Disabled
    public void shouldLoadHeaderInJson() {
        AuditConfig config = AuditConfig.load("audit-json");
        Assertions.assertTrue(config.hasAuditList());
        Assertions.assertEquals(10, config.getAuditList().size());
    }

}
