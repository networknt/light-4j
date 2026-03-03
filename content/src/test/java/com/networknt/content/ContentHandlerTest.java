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

package com.networknt.content;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Ricardo Pina Arellano on 13/06/18.
 */
public class ContentHandlerTest {
  private static final Logger logger = LoggerFactory.getLogger(ContentHandlerTest.class);

  private static Undertow server = null;
  private static final String url = "http://localhost:7080";

  @BeforeAll
  public static void setUp() {
    if (server == null) {
      logger.info("starting server");
      HttpHandler handler = getTestHandler();
      ContentHandler contentHandler = new ContentHandler();

      contentHandler.setNext(handler);

      handler = contentHandler;
      server = Undertow.builder()
        .addHttpListener(7080, "localhost")
        .setHandler(handler)
        .build();
      server.start();
    }
  }

  @AfterAll
  public static void tearDown() throws Exception {
    if (server != null) {
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
      .add(Methods.GET, "/", exchange -> {
        exchange.getResponseSender().send("This is just a proof");
      })
      .add(Methods.GET, "/xml", exchange -> {
        exchange
          .getResponseSender()
          .send("<bookstore><book><title>The best of Light-4j</title>" +
            "<author>Steve Hu</author><year>2018</year></book></bookstore>");
      })
      .add(Methods.GET, "/json", exchange -> {
        exchange.getResponseSender().send("{\"bookstore\":{\"book\":{\"title\":\"The best of Light-4j\",\"author\":\"Steve Hu\"}}}");
      });
  }

  @Test
  public void testTextPlainContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final SimpleConnectionState.ConnectionToken token;

    try {

        token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

    } catch (Exception e) {

        throw new ClientException(e);

    }

    final ClientConnection connection = (ClientConnection) token.getRawConnection();

    final AtomicReference<ClientResponse> reference = new AtomicReference<>();
    final String defaultContentType = "text/plain";
    final String defaultHeader = "Content-Type";

    try {
      final ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.GET);
      request.getRequestHeaders().put(Headers.HOST, "localhost");
      request.getRequestHeaders().put(new HttpString(defaultHeader), defaultContentType);
      connection.sendRequest(request, client.createClientCallback(reference, latch));
      latch.await();
    } catch (Exception e) {
      logger.error("Exception: ", e);
      throw new ClientException(e);
    } finally {

        client.restore(token);

    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst(defaultHeader);

    Assertions.assertEquals(200, statusCode);
    Assertions.assertNotNull(header);
    Assertions.assertEquals(header, defaultContentType);
  }

  @Test
  public void testXMLContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final SimpleConnectionState.ConnectionToken token;

    try {

        token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

    } catch (Exception e) {

        throw new ClientException(e);

    }

    final ClientConnection connection = (ClientConnection) token.getRawConnection();

    final AtomicReference<ClientResponse> reference = new AtomicReference<>();
    final String defaultContentType = "application/xml";
    final String defaultHeader = "Content-Type";

    try {
      final ClientRequest request = new ClientRequest().setPath("/xml").setMethod(Methods.GET);
      request.getRequestHeaders().put(Headers.HOST, "localhost");
      request.getRequestHeaders().put(new HttpString(defaultHeader), defaultContentType);
      connection.sendRequest(request, client.createClientCallback(reference, latch));
      latch.await();
    } catch (Exception e) {
      logger.error("Exception: ", e);
      throw new ClientException(e);
    } finally {

        client.restore(token);

    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst(defaultHeader);

    Assertions.assertEquals(200, statusCode);
    Assertions.assertNotNull(header);
    Assertions.assertEquals(header, defaultContentType);
  }

  @Test
  public void testJSONContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final SimpleConnectionState.ConnectionToken token;

    try {

        token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

    } catch (Exception e) {

        throw new ClientException(e);

    }

    final ClientConnection connection = (ClientConnection) token.getRawConnection();

    final AtomicReference<ClientResponse> reference = new AtomicReference<>();
    final String defaultContentType = "application/json";
    final String defaultHeader = "Content-Type";

    try {
      final ClientRequest request = new ClientRequest().setPath("/json").setMethod(Methods.GET);
      request.getRequestHeaders().put(Headers.HOST, "localhost");
      request.getRequestHeaders().put(new HttpString(defaultHeader), defaultContentType);
      connection.sendRequest(request, client.createClientCallback(reference, latch));
      latch.await();
    } catch (Exception e) {
      logger.error("Exception: ", e);
      throw new ClientException(e);
    } finally {

        client.restore(token);

    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst(defaultHeader);

    Assertions.assertEquals(200, statusCode);
    Assertions.assertNotNull(header);
    Assertions.assertEquals(header, defaultContentType);
  }

  @Test
  public void testDefaultContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final SimpleConnectionState.ConnectionToken token;

    try {

        token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

    } catch (Exception e) {

        throw new ClientException(e);

    }

    final ClientConnection connection = (ClientConnection) token.getRawConnection();

    final AtomicReference<ClientResponse> reference = new AtomicReference<>();

    try {
      final ClientRequest request = new ClientRequest().setPath("/json").setMethod(Methods.GET);
      request.getRequestHeaders().put(Headers.HOST, "localhost");
      connection.sendRequest(request, client.createClientCallback(reference, latch));
      latch.await();
    } catch (Exception e) {
      logger.error("Exception: ", e);
      throw new ClientException(e);
    } finally {

        client.restore(token);

    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst("Content-Type");

    Assertions.assertEquals(200, statusCode);
    Assertions.assertNotNull(header);
    Assertions.assertEquals(header, "application/json");
  }
}
