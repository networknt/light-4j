package com.networknt.service;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by stevehu on 2016-11-26.
 */
public class ServiceConfigTest {
    static String CONFIG_NAME = "service";
    @Test
    public void testServiceConfig() {

        ServiceConfig serviceConfig = (ServiceConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServiceConfig.class);
        List<String> singleton = serviceConfig.getSingletons();
        Assert.assertTrue(singleton.size() > 0);
    }
}
