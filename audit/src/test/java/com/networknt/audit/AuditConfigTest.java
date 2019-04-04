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

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.networknt.audit.AuditConfig.CONFIG_NAME;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {"com.networknt.config.Config", "org.slf4j.LoggerFactory"})
public class AuditConfigTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Config.class);
        PowerMockito.mockStatic(LoggerFactory.class);
    }

    @Test
    public void shouldLoadEmptyConfig() {
        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(LoggerFactory.getLogger(Constants.AUDIT_LOGGER)).thenReturn(logger);

        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getJsonMapConfigNoCache(CONFIG_NAME)).thenReturn(new HashMap<>());

        Mockito.when(Config.getInstance()).thenReturn(config);
        AuditConfig configHandler = AuditConfig.load();

        Assert.assertFalse(configHandler.hasAuditList());
        Assert.assertFalse(configHandler.hasHeaderList());
        Assert.assertNotNull(configHandler.getAuditFunc());
        Assert.assertNull(configHandler.getAuditList());
        Assert.assertNull(configHandler.getHeaderList());
        Assert.assertFalse(configHandler.isStatusCode());
        Assert.assertFalse(configHandler.isResponseTime());
        Assert.assertFalse(configHandler.isAuditOnError());
        Assert.assertFalse(configHandler.isMaskEnabled());
    }

    @Test
    public void shouldLoadFalseValuesConfig() {
        HashMap<String, Object> configMap = createMapValues(false);

        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(LoggerFactory.getLogger(Constants.AUDIT_LOGGER)).thenReturn(logger);

        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getJsonMapConfigNoCache(CONFIG_NAME)).thenReturn(configMap);
        Mockito.when(Config.getInstance()).thenReturn(config);

        AuditConfig configHandler = AuditConfig.load();

        Assert.assertFalse(configHandler.hasAuditList());
        Assert.assertFalse(configHandler.hasHeaderList());
        Assert.assertNotNull(configHandler.getAuditFunc());
        Assert.assertTrue(configHandler.getAuditList().isEmpty());
        Assert.assertTrue(configHandler.getHeaderList().isEmpty());
        Assert.assertFalse(configHandler.isStatusCode());
        Assert.assertFalse(configHandler.isResponseTime());
        Assert.assertFalse(configHandler.isAuditOnError());
        Assert.assertFalse(configHandler.isMaskEnabled());

        configHandler.getAuditFunc().accept("");
        Mockito.verify(logger, Mockito.never()).error(Mockito.anyString());
        Mockito.verify(logger).info(Mockito.anyString());
    }


    @Test
    public void shouldLoadTrueValuesConfig() {
        HashMap<String, Object> configMap = createMapValues(true);

        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(LoggerFactory.getLogger(Constants.AUDIT_LOGGER)).thenReturn(logger);

        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getJsonMapConfigNoCache(CONFIG_NAME)).thenReturn(configMap);
        Mockito.when(Config.getInstance()).thenReturn(config);

        AuditConfig configHandler = AuditConfig.load();

        Assert.assertFalse(configHandler.hasAuditList());
        Assert.assertFalse(configHandler.hasHeaderList());
        Assert.assertNotNull(configHandler.getAuditFunc());
        Assert.assertTrue(configHandler.getAuditList().isEmpty());
        Assert.assertTrue(configHandler.getHeaderList().isEmpty());
        Assert.assertTrue(configHandler.isStatusCode());
        Assert.assertTrue(configHandler.isResponseTime());
        Assert.assertTrue(configHandler.isAuditOnError());
        Assert.assertTrue(configHandler.isMaskEnabled());

        configHandler.getAuditFunc().accept("");
        Mockito.verify(logger).error(Mockito.anyString());
        Mockito.verify(logger, Mockito.never()).info(Mockito.anyString());
    }

    @Test
    public void shouldGetListOfStringsForHeadersAndAuditConfig() {
        List<Object> headers = Arrays.asList("header1", "header2");
        List<Object> audit = Arrays.asList("audit1", "audit2");

        HashMap<String, Object> configMap = new HashMap<>();
        configMap.put("headers", headers);
        configMap.put("audit", audit);

        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(LoggerFactory.getLogger(Constants.AUDIT_LOGGER)).thenReturn(logger);

        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getJsonMapConfigNoCache(CONFIG_NAME)).thenReturn(configMap);
        Mockito.when(Config.getInstance()).thenReturn(config);

        AuditConfig configHandler = AuditConfig.load();

        Assert.assertEquals(headers, configHandler.getHeaderList());
        Assert.assertEquals(audit, configHandler.getAuditList());
    }

    private HashMap<String, Object> createMapValues(boolean typeOfValue) {
        HashMap<String, Object> configMap = new HashMap<>();
        configMap.put("headers", Arrays.asList());
        configMap.put("audit", Arrays.asList());
        configMap.put("statusCode", typeOfValue);
        configMap.put("responseTime", typeOfValue);
        configMap.put("auditOnError", typeOfValue);
        configMap.put("logLevelIsError", typeOfValue);
        configMap.put("mask", typeOfValue);
        return configMap;
    }
}
