package com.networknt.content;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
  private static final String url = "http://localhost:8080";

  @BeforeClass
  public static void setUp() {
    if (server == null) {
      logger.info("starting server");
      HttpHandler handler = getTestHandler();
      ContentHandler contentHandler = new ContentHandler();

      contentHandler.setNext(handler);

      handler = contentHandler;
      server = Undertow.builder()
        .addHttpListener(8080, "localhost")
        .setHandler(handler)
        .build();
      server.start();
    }
  }

  @AfterClass
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
    final ClientConnection connection;

    try {
      connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
    } catch (Exception e) {
      throw new ClientException(e);
    }

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
      IoUtils.safeClose(connection);
    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst(defaultHeader);

    Assert.assertEquals(200, statusCode);
    Assert.assertNotNull(header);
    Assert.assertEquals(header, defaultContentType);
  }

  @Test
  public void testXMLContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final ClientConnection connection;

    try {
      connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
    } catch (Exception e) {
      throw new ClientException(e);
    }

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
      IoUtils.safeClose(connection);
    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst(defaultHeader);

    Assert.assertEquals(200, statusCode);
    Assert.assertNotNull(header);
    Assert.assertEquals(header, defaultContentType);
  }

  @Test
  public void testJSONContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final ClientConnection connection;

    try {
      connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
    } catch (Exception e) {
      throw new ClientException(e);
    }

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
      IoUtils.safeClose(connection);
    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst(defaultHeader);

    Assert.assertEquals(200, statusCode);
    Assert.assertNotNull(header);
    Assert.assertEquals(header, defaultContentType);
  }

  @Test
  public void testDefaultContentType() throws Exception {
    final Http2Client client = Http2Client.getInstance();
    final CountDownLatch latch = new CountDownLatch(1);
    final ClientConnection connection;

    try {
      connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
    } catch (Exception e) {
      throw new ClientException(e);
    }

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
      IoUtils.safeClose(connection);
    }

    final int statusCode = reference.get().getResponseCode();
    final HeaderMap headerMap = reference.get().getResponseHeaders();
    final String header = headerMap.getFirst("Content-Type");

    Assert.assertEquals(200, statusCode);
    Assert.assertNotNull(header);
    Assert.assertEquals(header, "application/json");
  }
}
