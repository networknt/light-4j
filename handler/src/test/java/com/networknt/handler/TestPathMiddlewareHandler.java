package com.networknt.handler;

import io.undertow.server.RoutingHandler;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Follows given-when-then format
 *
 * @author Nicholas Azar
 */
public class TestPathMiddlewareHandler {

    @Test
    public void validHandlers_requestedNotConfigured_NoneReturned() {
        PathMiddlewareHandler pathMiddlewareHandler = new PathMiddlewareHandler();
        pathMiddlewareHandler.setConfig("multiple-handler");
        pathMiddlewareHandler.setHandlerName("third");
        Assert.assertEquals(0, pathMiddlewareHandler.getHandlerPaths().size());
    }

    @Test
    public void multipleHandlerNames_pathsRetrieved_onlyMineReturned() {
        PathMiddlewareHandler pathMiddlewareHandler = new PathMiddlewareHandler();
        pathMiddlewareHandler.setConfig("multiple-handler");
        pathMiddlewareHandler.setHandlerName("second");
        Assert.assertEquals(1, pathMiddlewareHandler.getHandlerPaths().size());
    }

    @Test
    public void enabledConfig_whenIsEnabledCalled_returnsTrue() {
        PathMiddlewareHandler pathMiddlewareHandler = new PathMiddlewareHandler();
        pathMiddlewareHandler.setConfig("handler");
        Assert.assertTrue(pathMiddlewareHandler.isEnabled());
    }

    @Test
    public void disabledConfig_whenIsEnabledCalled_returnsFalse() {
        PathMiddlewareHandler pathMiddlewareHandler = new PathMiddlewareHandler();
        pathMiddlewareHandler.setConfig("disabled-handler");
        Assert.assertFalse(pathMiddlewareHandler.isEnabled());
    }

    @Test
    public void validConfig_noHandlerName_setsEmptyRoutingHandler() throws Exception {
        PathMiddlewareHandler pathMiddlewareHandler = new PathMiddlewareHandler();
        pathMiddlewareHandler.setConfig("handler");
        pathMiddlewareHandler.setNext(httpServerExchange -> {}); // noop
        Method method = RoutingHandler.class.getDeclaredMethod("getMatches");
        method.setAccessible(true);
        Map<String, Object> result = (Map<String, Object>) method.invoke(pathMiddlewareHandler.getNext());
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void validConfig_nextSet_isValid() throws Exception {
        PathMiddlewareHandler pathMiddlewareHandler = new PathMiddlewareHandler();
        pathMiddlewareHandler.setConfig("handler");
        pathMiddlewareHandler.setHandlerName("first");
        pathMiddlewareHandler.setNext(httpServerExchange -> {}); // noop
        Method method = RoutingHandler.class.getDeclaredMethod("getMatches");
        method.setAccessible(true);
        Map<String, Object> result = (Map<String, Object>) method.invoke(pathMiddlewareHandler.getNext());
        Assert.assertEquals(2, result.size());
    }


}
