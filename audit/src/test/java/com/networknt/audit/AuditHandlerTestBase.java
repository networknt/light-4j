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
import com.networknt.body.BodyHandler;
import com.networknt.client.Http2Client;
import com.networknt.config.JsonMapper;
import com.networknt.correlation.CorrelationHandler;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by steve on 01/09/16.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.*", "org.xml.sax.*", "org.apache.log4j.*", "java.xml.*", "com.sun.*"})
public abstract class AuditHandlerTestBase {
    static Logger logger = LoggerFactory.getLogger(AuditHandlerTestBase.class);

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

            BodyHandler bodyHandler = new BodyHandler();
            bodyHandler.setNext(handler);
            handler = bodyHandler;

            ParameterHandler parameterHandler = new ParameterHandler();
            parameterHandler.setNext(handler);
            handler = parameterHandler;

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
        System.out.println("afterclass");
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.POST, "/pet", exchange -> exchange.getResponseSender().send("OK"))
                .add(Methods.POST, "/error", exchange -> new ErrorStatusTestHandler().handleRequest(exchange))
                .add(Methods.POST, "/error/{petId}", exchange -> new ErrorStatusTestHandler().handleRequest(exchange))
                .add(Methods.POST, "/pet/{petId}", exchange -> exchange.getResponseSender().send("OK"));
    }

    @Before
    public void beforeTest() throws Exception {
        // inject the mock appender
        auditLogger.addAppender(mockAppender);
        System.out.println("before");
    }

    @After
    public void afterTest() throws Exception {
        // remove the mock appender
        auditLogger.detachAppender(mockAppender);
        System.out.println("after");
    }

    protected void verifyAuditLog(String traceVal) {
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        ILoggingEvent event = captorLoggingEvent.getValue();
        Map<String, Object> mapValue = JsonMapper.string2Map(event.getFormattedMessage());

        Assert.assertEquals(Level.INFO, event.getLevel());
        Assert.assertTrue(Arrays.stream(requiredKeys).allMatch(mapValue::containsKey));
        Assert.assertEquals(traceVal, mapValue.get(Constants.TRACEABILITY_ID_STRING));
        Assert.assertNotNull(mapValue.get(Constants.CORRELATION_ID_STRING));
        Assert.assertEquals(200, mapValue.get(AuditHandler.STATUS_CODE));
    }

    protected void verifyAuditInfo(String key, String value) {
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        ILoggingEvent event = captorLoggingEvent.getValue();
        Map<String, Object> mapValue = JsonMapper.string2Map(event.getFormattedMessage());
        if (value == null) {
            Assert.assertNull(mapValue.get(key));
        } else {
            Assert.assertEquals(value, mapValue.get(key));
        }
    }

    protected void runTest(String path, String body, String cookies, int expectStatus) throws ClientException {
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
            String post = body;
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(path);
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, "tid");
                    request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
                    request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    if (cookies != null) {
                        request.getRequestHeaders().put(Headers.COOKIE, cookies);
                    }
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
        Assert.assertEquals(expectStatus, reference.get().getResponseCode());

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }
}
