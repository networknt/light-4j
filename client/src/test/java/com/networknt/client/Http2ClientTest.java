package com.networknt.client;

import com.networknt.config.Config;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.io.Receiver;
import io.undertow.io.Sender;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xnio.*;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Http2ClientTest {
    static Undertow server = null;
    static SSLContext sslContext;
    private static final String message = "Hello World!";
    public static final String MESSAGE = "/message";
    public static final String POST = "/post";
    public static final String FORM = "/form";

    private static final String SERVER_KEY_STORE = "tls/server.keystore";
    private static final String SERVER_TRUST_STORE = "tls/server.truststore";
    private static final String CLIENT_KEY_STORE = "tls/client.keystore";
    private static final String CLIENT_TRUST_STORE = "tls/client.truststore";
    private static final char[] STORE_PASSWORD = "password".toCharArray();

    private static XnioWorker worker;

    private static final URI ADDRESS;


    private static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);

    static {
        try {
            ADDRESS = new URI("http://localhost:7777");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendMessage(final HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, message.length() + "");
        final Sender sender = exchange.getResponseSender();
        sender.send(message);
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        // Create xnio worker
        final Xnio xnio = Xnio.getInstance();
        final XnioWorker xnioWorker = xnio.createWorker(null, Http2Client.DEFAULT_OPTIONS);
        worker = xnioWorker;

        if(server == null) {
            System.out.println("starting server");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            builder.addHttpsListener(7778, "localhost", sslContext);
            builder.addHttpListener(7777, "localhost");

            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);


            server = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(MESSAGE, new HttpHandler() {
                                @Override
                                public void handleRequest(HttpServerExchange exchange) throws Exception {
                                    sendMessage(exchange);
                                }
                            })
                            .addExactPath(FORM, new HttpHandler() {
                                @Override
                                public void handleRequest(HttpServerExchange exchange) throws Exception {
                                    exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                        @Override
                                        public void handle(HttpServerExchange exchange, String message) {
                                            exchange.getResponseSender().send(message);
                                        }
                                    });
                                }
                            })
                            .addExactPath(POST, new HttpHandler() {
                                @Override
                                public void handleRequest(HttpServerExchange exchange) throws Exception {
                                    exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                        @Override
                                        public void handle(HttpServerExchange exchange, String message) {
                                            exchange.getResponseSender().send(message);
                                        }
                                    });
                                }
                            }))
                    .setWorkerThreads(200)
                    .build();

            server.start();
        }

    }

    @AfterClass
    public static void afterClass() {
        worker.shutdown();
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server.stop();
            System.out.println("The server is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

    }

    static Http2Client createClient() {
        return createClient(OptionMap.EMPTY);
    }

    static Http2Client createClient(final OptionMap options) {
        return Http2Client.getInstance();
    }

    @Test
    public void testMultipleHttpGet() throws Exception {
        //
        final Http2Client client = createClient();

        final List<ClientResponse> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        final ClientConnection connection = client.connect(ADDRESS, worker, Http2Client.POOL, OptionMap.EMPTY).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(MESSAGE);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, createClientCallback(responses, latch));
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final ClientResponse response : responses) {
                Assert.assertEquals(message, response.getAttachment(RESPONSE_BODY));
                Assert.assertEquals("HTTP/1.1", response.getProtocol().toString());
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    /*
    @Test
    public void testMultipleHttp2Get() throws Exception {
        //
        final Http2Client client = createClient();

        final List<ClientResponse> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        final ClientConnection connection = client.connect(ADDRESS, worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(MESSAGE);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, createClientCallback(responses, latch));
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final ClientResponse response : responses) {
                Assert.assertEquals(message, response.getAttachment(RESPONSE_BODY));
                Assert.assertEquals("HTTP/1.1", response.getProtocol().toString());
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }
    */

    @Test
    public void testMultipleHttpPost() throws Exception {
        //
        final Http2Client client = createClient();
        final String postMessage = "This is a post request";

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        final ClientConnection connection = client.connect(ADDRESS, worker, Http2Client.POOL, OptionMap.EMPTY).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                        connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                                result.setResponseListener(new ClientCallback<ClientExchange>() {
                                    @Override
                                    public void completed(ClientExchange result) {
                                        new StringReadChannelListener(Http2Client.POOL) {

                                            @Override
                                            protected void stringDone(String string) {
                                                responses.add(string);
                                                latch.countDown();
                                            }

                                            @Override
                                            protected void error(IOException e) {
                                                e.printStackTrace();
                                                latch.countDown();
                                            }
                                        }.setup(result.getResponseChannel());
                                    }

                                    @Override
                                    public void failed(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                });
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    /*
    @Test
    public void testMultipleHttp2Post() throws Exception {
        //
        final Http2Client client = createClient();
        final String postMessage = "This is a post request";

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        final ClientConnection connection = client.connect(ADDRESS, worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                        connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                                result.setResponseListener(new ClientCallback<ClientExchange>() {
                                    @Override
                                    public void completed(ClientExchange result) {
                                        new StringReadChannelListener(pool) {

                                            @Override
                                            protected void stringDone(String string) {
                                                responses.add(string);
                                                latch.countDown();
                                            }

                                            @Override
                                            protected void error(IOException e) {
                                                e.printStackTrace();
                                                latch.countDown();
                                            }
                                        }.setup(result.getResponseChannel());
                                    }

                                    @Override
                                    public void failed(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                });
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }
    */

    @Test
    public void testMultipleHttpGetSsl() throws Exception {
        //
        final Http2Client client = createClient();

        final List<ClientResponse> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.SSL_BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.POOL, OptionMap.EMPTY).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(MESSAGE);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, createClientCallback(responses, latch));
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final ClientResponse response : responses) {
                Assert.assertEquals(message, response.getAttachment(RESPONSE_BODY));
                Assert.assertEquals("HTTP/1.1", response.getProtocol().toString());
            }
        } finally {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    IoUtils.safeClose(connection);
                }
            });
        }
    }

    @Test
    public void testMultipleHttp2GetSsl() throws Exception {
        //
        final Http2Client client = createClient();

        final List<ClientResponse> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.SSL_BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(MESSAGE);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, createClientCallback(responses, latch));
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final ClientResponse response : responses) {
                Assert.assertEquals(message, response.getAttachment(RESPONSE_BODY));
                Assert.assertEquals("HTTP/2.0", response.getProtocol().toString());
            }
        } finally {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    IoUtils.safeClose(connection);
                }
            });
        }
    }


    @Test
    public void testMultipleHttpPostSsl() throws Exception {
        //
        final Http2Client client = createClient();
        final String postMessage = "This is a post request";

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.SSL_BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.POOL, OptionMap.EMPTY).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                        connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                                result.setResponseListener(new ClientCallback<ClientExchange>() {
                                    @Override
                                    public void completed(ClientExchange result) {
                                        new StringReadChannelListener(Http2Client.POOL) {

                                            @Override
                                            protected void stringDone(String string) {
                                                responses.add(string);
                                                latch.countDown();
                                            }

                                            @Override
                                            protected void error(IOException e) {
                                                e.printStackTrace();
                                                latch.countDown();
                                            }
                                        }.setup(result.getResponseChannel());
                                    }

                                    @Override
                                    public void failed(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                });
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testMultipleHttp2PostSsl() throws Exception {
        //
        final Http2Client client = createClient();
        final String postMessage = "This is a post request";

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(10);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.SSL_BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                        connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                                result.setResponseListener(new ClientCallback<ClientExchange>() {
                                    @Override
                                    public void completed(ClientExchange result) {
                                        new StringReadChannelListener(Http2Client.POOL) {

                                            @Override
                                            protected void stringDone(String string) {
                                                responses.add(string);
                                                latch.countDown();
                                            }

                                            @Override
                                            protected void error(IOException e) {
                                                e.printStackTrace();
                                                latch.countDown();
                                            }
                                        }.setup(result.getResponseChannel());
                                    }

                                    @Override
                                    public void failed(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                });
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }
                }

            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(10, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testSingleHttp2PostSsl() throws Exception {
        //
        final Http2Client client = createClient();
        final String postMessage = "This is a post request";

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.SSL_BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                            result.setResponseListener(new ClientCallback<ClientExchange>() {
                                @Override
                                public void completed(ClientExchange result) {
                                    new StringReadChannelListener(Http2Client.POOL) {

                                        @Override
                                        protected void stringDone(String string) {
                                            responses.add(string);
                                            latch.countDown();
                                        }

                                        @Override
                                        protected void error(IOException e) {
                                            e.printStackTrace();
                                            latch.countDown();
                                        }
                                    }.setup(result.getResponseChannel());
                                }

                                @Override
                                public void failed(IOException e) {
                                    e.printStackTrace();
                                    latch.countDown();
                                }
                            });
                        }

                        @Override
                        public void failed(IOException e) {
                            e.printStackTrace();
                            latch.countDown();
                        }
                    });
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(1, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testSingleHttp2FormSsl() throws Exception {
        //
        final Http2Client client = createClient();
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");

        final String postMessage = client.getFormDataString(params);

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.SSL_BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(FORM);
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
                    connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                            result.setResponseListener(new ClientCallback<ClientExchange>() {
                                @Override
                                public void completed(ClientExchange result) {
                                    new StringReadChannelListener(Http2Client.POOL) {

                                        @Override
                                        protected void stringDone(String string) {
                                            System.out.println("string = " + string);
                                            responses.add(string);
                                            latch.countDown();
                                        }

                                        @Override
                                        protected void error(IOException e) {
                                            e.printStackTrace();
                                            latch.countDown();
                                        }
                                    }.setup(result.getResponseChannel());
                                }

                                @Override
                                public void failed(IOException e) {
                                    e.printStackTrace();
                                    latch.countDown();
                                }
                            });
                        }

                        @Override
                        public void failed(IOException e) {
                            e.printStackTrace();
                            latch.countDown();
                        }
                    });
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(1, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testConnectionClose() throws Exception {
        //
        final Http2Client client = createClient();

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = client.connect(ADDRESS, worker, Http2Client.POOL, OptionMap.EMPTY).get();
        try {
            ClientRequest request = new ClientRequest().setPath(MESSAGE).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            final List<ClientResponse> responses = new CopyOnWriteArrayList<>();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, createClientCallback(responses, latch));
            latch.await();
            final ClientResponse response = responses.iterator().next();
            Assert.assertEquals(message, response.getAttachment(RESPONSE_BODY));
            Assert.assertEquals(false, connection.isOpen());
        } finally {
            IoUtils.safeClose(connection);
        }

    }

    private ClientCallback<ClientExchange> createClientCallback(final List<ClientResponse> responses, final CountDownLatch latch) {
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        responses.add(result.getResponse());
                        new StringReadChannelListener(result.getConnection().getBufferPool()) {

                            @Override
                            protected void stringDone(String string) {
                                result.getResponse().putAttachment(RESPONSE_BODY, string);
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                e.printStackTrace();

                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        e.printStackTrace();

                        latch.countDown();
                    }
                });
                try {
                    result.getRequestChannel().shutdownWrites();
                    if(!result.getRequestChannel().flush()) {
                        result.getRequestChannel().getWriteSetter().set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
                        result.getRequestChannel().resumeWrites();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public void failed(IOException e) {
                e.printStackTrace();
                latch.countDown();
            }
        };
    }


    private static KeyStore loadKeyStore(final String name) throws IOException {
        final InputStream stream = Config.getInstance().getInputStreamFromFile(name);
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, STORE_PASSWORD);

            return loadedKeystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, boolean client) throws IOException {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, STORE_PASSWORD);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        }

        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        }

        SSLContext sslContext;
        try {
            if(!client) {
                sslContext = SSLContext.getInstance("TLS");
            } else {
                sslContext = SSLContext.getInstance("TLS");
            }
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }
}
