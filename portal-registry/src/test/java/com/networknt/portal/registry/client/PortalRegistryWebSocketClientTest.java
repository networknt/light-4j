package com.networknt.portal.registry.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalRegistryWebSocketClientTest {

    @Test
    void testUnmatchedResponseIsNotForwardedToMessageHandler() throws Exception {
        PortalRegistryWebSocketClient client =
                new PortalRegistryWebSocketClient(URI.create("wss://localhost:8443/ws/microservice"), null);
        AtomicInteger handled = new AtomicInteger();
        client.setMessageHandler((ignoredClient, ignoredEnvelope) -> handled.incrementAndGet());

        Class<?> listenerClass = Class.forName(
                "com.networknt.portal.registry.client.PortalRegistryWebSocketClient$PortalWebSocketListener");
        Constructor<?> constructor = listenerClass.getDeclaredConstructor(PortalRegistryWebSocketClient.class);
        constructor.setAccessible(true);
        Object listener = constructor.newInstance(client);

        Method handleMessage = listenerClass.getDeclaredMethod("handleMessage", String.class);
        handleMessage.setAccessible(true);
        handleMessage.invoke(listener, "{\"jsonrpc\":\"2.0\",\"id\":\"late-1\",\"result\":{\"ok\":true}}");

        assertEquals(0, handled.get());
    }
}
