package com.networknt.consul;

import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulTest {
    private ConsulRegistry registry;
    private URL serviceUrl;
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        registry = (ConsulRegistry)SingletonServiceFactory.getBean(Registry.class);


        serviceUrl = getMockUrl("http", "192.168.1.119",8083, "MockService");

        sleepTime = ConsulConstants.SWITCHER_CHECK_CIRCLE + 500;
    }

    @After
    public void tearDown() throws Exception {
        registry = null;
    }

    //@Test
    public void doRegisterAndAvailable() throws Exception {
        // register
        registry.doRegister(serviceUrl);

        // unregister
        registry.doUnregister(serviceUrl);
    }

    //@Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> urls = registry.discoverService(serviceUrl);
        Assert.assertNull(urls);
        Thread.sleep(sleepTime);
        urls = registry.discoverService(serviceUrl);
        //Assert.assertTrue(urls.contains(serviceUrl));
    }

    public static URL getMockUrl(String protocol, String address, int port, String serviceName) {
        Map<String, String> params = new HashMap<>();
        params.put("environment", "test1");
        URL url = new URLImpl(protocol, address, port, serviceName, params);
        return url;
    }
}
