package com.networknt.security;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UndertowVerifyHandlerTest {
    private final UndertowVerifyHandler handler = new UndertowVerifyHandler();

    @Test
    public void testCheckForH2CRequest_H2C() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "h2c");
        headerMap.put(Headers.CONNECTION, "Upgrade");
        Assertions.assertTrue(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_H2() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "h2");
        headerMap.put(Headers.CONNECTION, "Upgrade");
        Assertions.assertTrue(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_WebSocket() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "websocket");
        headerMap.put(Headers.CONNECTION, "Upgrade");
        Assertions.assertFalse(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_NonWebSocketUpgrade() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "foo/2");
        headerMap.put(Headers.CONNECTION, "Upgrade");
        Assertions.assertTrue(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_NoUpgrade() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.CONNECTION, "Upgrade");
        Assertions.assertFalse(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_NoConnection() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "h2c");
        Assertions.assertFalse(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_WrongConnection() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "h2c");
        headerMap.put(Headers.CONNECTION, "keep-alive");
        Assertions.assertFalse(handler.checkForH2CRequest(headerMap));
    }

    @Test
    public void testCheckForH2CRequest_CaseInsensitive() {
        HeaderMap headerMap = new HeaderMap();
        headerMap.put(Headers.UPGRADE, "H2C");
        headerMap.put(Headers.CONNECTION, "upgrade");
        Assertions.assertTrue(handler.checkForH2CRequest(headerMap));
    }
}
