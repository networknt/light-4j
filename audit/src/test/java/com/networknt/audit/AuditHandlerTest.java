/*
 * Copyright (c) 2016 Network New Technologies Inc.
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.correlation.CorrelationHandler;
import com.networknt.exception.ClientException;
import com.networknt.handler.Handler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by steve on 01/09/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AuditConfig.class, LoggerFactory.class})
@PowerMockIgnore({"javax.*", "org.xml.sax.*", "org.apache.log4j.*", "java.xml.*", "com.sun.*"})
public class AuditHandlerTest {
    static Logger logger = LoggerFactory.getLogger(AuditHandlerTest.class);

    static Undertow server = null;

    final ch.qos.logback.classic.Logger auditLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Constants.AUDIT_LOGGER);

    @Mock
    Appender mockAppender;

    @Captor
    ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    String[] requiredKeys = { AuditHandler.TIMESTAMP,
                                Constants.CORRELATION_ID_STRING,
                                Constants.TRACEABILITY_ID_STRING,
                                AuditHandler.STATUS_CODE,
                                AuditHandler.RESPONSE_TIME };

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();

            AuditHandler auditHandler = new AuditHandler();
            auditHandler.setNext(handler);
            handler = auditHandler;

            CorrelationHandler correlationHandler = new CorrelationHandler();
            correlationHandler.setNext(handler);
            handler = correlationHandler;

            server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.POST, "/pet", exchange -> exchange.getResponseSender().send("OK"));
    }

    @Before
    public void beforeTest() throws Exception {
        // inject the mock appender
        auditLogger.addAppender(mockAppender);
    }

    @After
    public void afterTest() throws Exception {
        // remove the mock appender
        auditLogger.detachAppender(mockAppender);
    }

    private void verifyAuditLog(String traceVal) {
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        ILoggingEvent event = captorLoggingEvent.getValue();
        Map<String, Object> mapValue = JsonMapper.string2Map(event.getFormattedMessage());

        Assert.assertEquals(Level.INFO, event.getLevel());
        Assert.assertTrue(Arrays.stream(requiredKeys).allMatch(mapValue::containsKey));
        Assert.assertEquals(traceVal, mapValue.get(Constants.TRACEABILITY_ID_STRING));
        Assert.assertNotNull(mapValue.get(Constants.CORRELATION_ID_STRING));
        Assert.assertEquals(200, mapValue.get(AuditHandler.STATUS_CODE));
    }

    @Test
    public void testAuditWithTrace() throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            String post = "post";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/pet");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, "tid");
                    request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
                    request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    connection.sendRequest(request, client.createClientCallback(reference, latch, post));
                }
            });

            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        Assert.assertEquals("OK", reference.get().getAttachment(Http2Client.RESPONSE_BODY));

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        verifyAuditLog("tid");
    }

    @Test
    public void testAuditWithoutTrace() throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            String post = "post";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/pet");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
                    request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    connection.sendRequest(request, client.createClientCallback(reference, latch, post));
                }
            });

            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        Assert.assertEquals("OK", reference.get().getAttachment(Http2Client.RESPONSE_BODY));

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        verifyAuditLog(null);
    }

    @Test
    public void shouldAddListenerIfIsStatusCodeAndIsResponseTimeAreTrue() throws Exception {
        PowerMockito.mockStatic(AuditConfig.class);

        AuditConfig configHandler = Mockito.mock(AuditConfig.class);
        Mockito.when(configHandler.isResponseTime()).thenReturn(true);
        Mockito.when(configHandler.isStatusCode()).thenReturn(true);

        Mockito.when(AuditConfig.load()).thenReturn(configHandler);

        HeaderMap headerMap = Mockito.spy(new HeaderMap());
        HttpServerExchange httpServerExchange = Mockito.mock(HttpServerExchange.class);
        Mockito.when(httpServerExchange.getRequestHeaders()).thenReturn(headerMap);

        AuditHandler auditHandler = Mockito.spy(new AuditHandler());
        Mockito.doNothing().when(auditHandler).next(Mockito.any());

        Handler.init();

        auditHandler.handleRequest(httpServerExchange);
        Mockito.verify(httpServerExchange).addExchangeCompleteListener(Mockito.any());
    }

    @Test
    public void shouldAddListenerIfIsStatusCodeIsFalseAndIsResponseTimeIsTrue() throws Exception {
        PowerMockito.mockStatic(AuditConfig.class);

        AuditConfig configHandler = Mockito.mock(AuditConfig.class);
        Mockito.when(configHandler.isResponseTime()).thenReturn(true);
        Mockito.when(configHandler.isStatusCode()).thenReturn(false);

        Mockito.when(AuditConfig.load()).thenReturn(configHandler);

        HeaderMap headerMap = Mockito.spy(new HeaderMap());
        HttpServerExchange httpServerExchange = Mockito.mock(HttpServerExchange.class);
        Mockito.when(httpServerExchange.getRequestHeaders()).thenReturn(headerMap);

        AuditHandler auditHandler = Mockito.spy(new AuditHandler());
        Mockito.doNothing().when(auditHandler).next(Mockito.any());

        Handler.init();

        auditHandler.handleRequest(httpServerExchange);
        Mockito.verify(httpServerExchange).addExchangeCompleteListener(Mockito.any());
    }

    @Test
    public void shouldAddListenerIfIsStatusCodeIsTrueAndIsResponseTimeIsFalse() throws Exception {
        PowerMockito.mockStatic(AuditConfig.class);

        AuditConfig configHandler = Mockito.mock(AuditConfig.class);
        Mockito.when(configHandler.isResponseTime()).thenReturn(false);
        Mockito.when(configHandler.isStatusCode()).thenReturn(true);

        Mockito.when(AuditConfig.load()).thenReturn(configHandler);

        HeaderMap headerMap = Mockito.spy(new HeaderMap());
        HttpServerExchange httpServerExchange = Mockito.mock(HttpServerExchange.class);
        Mockito.when(httpServerExchange.getRequestHeaders()).thenReturn(headerMap);

        AuditHandler auditHandler = Mockito.spy(new AuditHandler());
        Mockito.doNothing().when(auditHandler).next(Mockito.any());

        Handler.init();

        auditHandler.handleRequest(httpServerExchange);
        Mockito.verify(httpServerExchange).addExchangeCompleteListener(Mockito.any());
    }

    @Test
    public void shouldNotAddListenerIfStatusCodeAndResponseTimeAreFalse() throws Exception {
        PowerMockito.mockStatic(AuditConfig.class);
        Consumer<String> auditFunc= (Consumer<String>) Mockito.spy(Consumer.class);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);

        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getMapper()).thenReturn(objectMapper);

        AuditConfig configHandler = Mockito.mock(AuditConfig.class);
        Mockito.when(configHandler.isResponseTime()).thenReturn(false);
        Mockito.when(configHandler.isStatusCode()).thenReturn(false);
        Mockito.when(configHandler.getAuditFunc()).thenReturn(auditFunc);
        Mockito.when(configHandler.getConfig()).thenReturn(config);

        Mockito.when(AuditConfig.load()).thenReturn(configHandler);

        HttpServerExchange httpServerExchange = Mockito.mock(HttpServerExchange.class);

        AuditHandler auditHandler = Mockito.spy(new AuditHandler());
        Mockito.doNothing().when(auditHandler).next(Mockito.any());

        Handler.init();

        auditHandler.handleRequest(httpServerExchange);
        Mockito.verify(httpServerExchange, Mockito.never()).addExchangeCompleteListener(Mockito.any());
        Mockito.verify(auditFunc).accept(Mockito.any());
        Mockito.verify(objectMapper).writeValueAsString(Mockito.any());
    }

    private static class ArgumentMatcherAuditInfo implements ArgumentMatcher<AttachmentKey<Map>> {

        @Override
        public boolean matches(AttachmentKey<Map> attachmentKey) {
            if (attachmentKey == null) {
                return false;
            }
            return attachmentKey.toString().equals("io.undertow.util.SimpleAttachmentKey<java.util.Map>");
        }
    }

    private static class ArgumentMatcherChainId implements ArgumentMatcher<AttachmentKey<String>> {

        @Override
        public boolean matches(AttachmentKey<String> attachmentKey) {
            if (attachmentKey == null) {
                return false;
            }
            return attachmentKey.toString().equals("io.undertow.util.SimpleAttachmentKey<java.lang.String>");
        }
    }

    private static class ArgumentMatcherChainSeq implements ArgumentMatcher<AttachmentKey<Integer>> {

        @Override
        public boolean matches(AttachmentKey<Integer> attachmentKey) {
            if (attachmentKey == null) {
                return false;
            }
            return attachmentKey.toString().equals("io.undertow.util.SimpleAttachmentKey<java.lang.Integer>");
        }
    }
}
