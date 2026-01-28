package com.networknt.proxy;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;
import java.lang.reflect.Field;

public class LightProxyHandlerTest {
    @Test
    public void testReload() throws Exception {
        // Ensure config is loaded initially
        ProxyConfig.load();

        LightProxyHandler handler = new LightProxyHandler();

        // Get initial proxyHandler
        Field proxyHandlerField = LightProxyHandler.class.getDeclaredField("proxyHandler");
        proxyHandlerField.setAccessible(true);
        Object initialProxyHandler = proxyHandlerField.get(handler);

        // Force config reload by clearing cache
        Config.getInstance().clear();

        // Trigger handleRequest to check for reload
        try {
            handler.handleRequest(null);
        } catch (NullPointerException e) {
            // Expected as exchange is null, but rebuild logic happens before exchange usage (mostly)
            // Actually handleRequest accesses exchange early for logging or JWT?
            // "if(config.isForwardJwtClaims())" -> checks config.
            // "HeaderMap headerValues = exchange.getRequestHeaders();" -> null pointer if forwardJwtClaims is true.
            // Default forwardJwtClaims is false.
        } catch (Exception e) {
            // Ignore other exceptions
        }

        // Get new proxyHandler
        Object newProxyHandler = proxyHandlerField.get(handler);

        Assert.assertNotEquals("ProxyHandler should have been rebuilt after config reload", initialProxyHandler, newProxyHandler);
    }
}
