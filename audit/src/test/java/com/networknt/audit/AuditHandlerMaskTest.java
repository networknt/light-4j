package com.networknt.audit;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.networknt.body.BodyHandler;
import com.networknt.client.Http2Client;
import com.networknt.config.JsonMapper;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * This is the unit test class for Mask in Audit log.
 * @author John Su
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AuditConfig.class})
@PowerMockIgnore({"javax.*", "org.xml.sax.*", "org.apache.log4j.*", "java.xml.*", "com.sun.*"})
public class AuditHandlerMaskTest {

    private static Logger logger = LoggerFactory.getLogger(AuditHandlerMaskTest.class);
    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 8088;

    static Undertow server = null;

    static LogConsumer logConsumer = new LogConsumer();

    @BeforeClass
    public static void setUp() {
        PowerMockito.mockStatic(AuditConfig.class);

        AuditConfig configHandler = Mockito.mock(AuditConfig.class);
        Mockito.when(configHandler.isResponseTime()).thenReturn(true);
        Mockito.when(configHandler.isStatusCode()).thenReturn(true);
        Mockito.when(configHandler.isMaskEnabled()).thenReturn(true);

        List<String> auditList = Collections.singletonList("requestBody");
        Mockito.when(configHandler.hasAuditList()).thenReturn(true);
        Mockito.when(configHandler.getAuditList()).thenReturn(auditList);

        Mockito.when(configHandler.getAuditFunc()).thenReturn(logConsumer.getConsumer());

        Mockito.when(AuditConfig.load()).thenReturn(configHandler);

        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            public JsonProvider jsonProvider() {
                return this.jsonProvider;
            }

            public MappingProvider mappingProvider() {
                return this.mappingProvider;
            }

            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });

        HttpHandler handler = Handlers.routing()
                .add(Methods.POST, "/", exchange -> exchange.getResponseSender().send("OK"));

        BodyHandler bodyHandler = new BodyHandler();
        bodyHandler.setNext(handler);
        handler = bodyHandler;

        AuditHandler auditHandler = new AuditHandler();
        auditHandler.setNext(handler);
        handler = auditHandler;

        Undertow server = Undertow.builder()
                .addHttpListener(SERVER_PORT, SERVER_HOST)
                .setHandler(handler)
                .build();
        server.start();
        logger.info("The server is started at {}:{}.", SERVER_HOST, SERVER_PORT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            logger.info("The server is stopped.");
        }
    }

    private String sendRequest(String requestBody, String contentType) throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(String.format("http://%s:%d", SERVER_HOST, SERVER_PORT)),
                    Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/");
                request.getRequestHeaders().put(Headers.HOST, SERVER_HOST);
                if (contentType != null) {
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, contentType);
                }
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            });

            latch.await(10, TimeUnit.SECONDS);
            return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testMaskJsonRequestBodyWithoutSecret() throws Exception {
        logConsumer.clear();
        String requestBody = "{\"account\":{\"name\":\"John\"}}";
        Assert.assertEquals("OK", sendRequest(requestBody, "application/json"));

        assertRequestBody(200, requestBody);
    }

    @Test
    public void testMaskJsonInvalidRequestBodyWithoutSecret() throws Exception {
        logConsumer.clear();
        String requestBody = "{\"account\":\"name\":\"John\"}}";
        Assert.assertNotEquals("OK", sendRequest(requestBody, "application/json"));

        assertRequestBody(400, requestBody);
    }

    @Test
    public void testMaskJsonRequestBodyWithSecret() throws Exception {
        logConsumer.clear();
        String requestBody = "{\"account\":{\"password\":\"secret\"}}";
        Assert.assertEquals("OK", sendRequest(requestBody, "application/json"));

        assertRequestBody(200, requestBody.replace("secret", "******"));
    }

    @Test
    public void testMaskJsonInvalidRequestBodyWithSecret() throws Exception {
        logConsumer.clear();
        String requestBody = "{\"account\":\"password\":\"secret\"}}";
        Assert.assertNotEquals("OK", sendRequest(requestBody, "application/json"));

        assertRequestBody(400, requestBody);
    }

    @Test
    public void testMaskStringRequestBodyWithoutSecret() throws Exception {
        logConsumer.clear();
        String requestBody = "account=John Smith";
        Assert.assertEquals("OK", sendRequest(requestBody, "text/plain"));

        assertRequestBody(200, requestBody);
    }

    @Test
    public void testMaskStringRequestBodyWithSecret() throws Exception {
        logConsumer.clear();
        String requestBody = "password=secret";
        Assert.assertEquals("OK", sendRequest(requestBody, "text/plain"));

        assertRequestBody(200, requestBody.replace("secret", "******"));
    }

    private void assertRequestBody(int expectedStatusCode, String expectedRequestBody) throws Exception {
        int count = 0;
        while (count++ < 10 && logConsumer.getLoggedText() == null) {
            Thread.sleep(100);
        }
        String actualRequestBody = logConsumer.getLoggedText();

        Assert.assertNotNull(actualRequestBody);
        Map<String, Object> mapValue = JsonMapper.string2Map(actualRequestBody);
        Assert.assertNotNull(mapValue);
        Assert.assertEquals(expectedStatusCode, mapValue.get("statusCode"));
        Assert.assertEquals(expectedRequestBody, mapValue.get("requestBody"));
    }

    static class LogConsumer {

        private String loggedText;

        Consumer<String> getConsumer() {
            return str -> loggedText = str;
        }

        String getLoggedText() {
            return loggedText;
        }

        void clear() {
            loggedText = null;
        }
    }
}
