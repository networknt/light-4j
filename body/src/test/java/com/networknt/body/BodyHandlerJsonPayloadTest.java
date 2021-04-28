package com.networknt.body;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import io.undertow.io.BlockingSenderImpl;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the mocked unit test for BodyHandler with Json payload
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class, Handler.class, HttpServerExchange.class})
@PowerMockIgnore({"javax.*", "org.xml.sax.*", "org.apache.log4j.*", "java.xml.*", "com.sun.*"})
public class BodyHandlerJsonPayloadTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Handler.class);
    }

    @Test
    public void testPostValidJsonBody() throws Exception {

        final HttpServerExchange exchange = PowerMockito.mock(HttpServerExchange.class);

        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.CONTENT_TYPE, "application/json");
        PowerMockito.when(exchange.getRequestHeaders()).thenReturn(headerMap);

        String requestBodyString = "{\"key1\":{\"key2\":\"value2\"}}";
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(requestBodyString.getBytes());
        PowerMockito.when(exchange.getInputStream()).thenReturn(requestBodyStream);

        PowerMockito.when(exchange.putAttachment(Mockito.any(), Mockito.any())).thenCallRealMethod();
        PowerMockito.when(exchange.getAttachment(Mockito.any())).thenCallRealMethod();

        BodyHandler bodyHandler = new BodyHandler();
        bodyHandler.handleRequest(exchange);

        Object requestBody = exchange.getAttachment(BodyHandler.REQUEST_BODY);
        Assert.assertNotNull(requestBody);
        Assert.assertEquals(requestBodyString, Config.getInstance().getMapper().writeValueAsString(requestBody));
    }

    @Test
    public void testPostInvalidJsonBody() throws Exception {

        final HttpServerExchange exchange = PowerMockito.mock(HttpServerExchange.class);

        HeaderMap requestHeaders = new HeaderMap();
        requestHeaders.put(Headers.CONTENT_TYPE, "application/json");
        PowerMockito.when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        HeaderMap responseHeaders = new HeaderMap();
        PowerMockito.when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        BlockingSenderImpl blockingSender = new BlockingSenderImpl(exchange, new ByteArrayOutputStream());
        PowerMockito.when(exchange.getResponseSender()).thenReturn(blockingSender);

        String requestBodyString = "{\"key1\":\"key2\":\"value2\"}}";
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(requestBodyString.getBytes());
        PowerMockito.when(exchange.getInputStream()).thenReturn(requestBodyStream);

        PowerMockito.when(exchange.putAttachment(Mockito.any(), Mockito.any())).thenCallRealMethod();
        PowerMockito.when(exchange.getAttachment(Mockito.any())).thenCallRealMethod();

        BodyHandler bodyHandler = new BodyHandler();
        bodyHandler.handleRequest(exchange);

        String requestBody = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
        Assert.assertNotNull(requestBody);
        Assert.assertEquals(requestBodyString, requestBody);
    }
}
