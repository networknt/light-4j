package com.networknt.client.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.io.Receiver;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RestClientTemplateTest {

    public  static final Logger logger = LoggerFactory.getLogger(RestClientTemplateTest.class);

    static Undertow server = null;
    static SSLContext sslContext;
    private static final String SERVER_KEY_STORE = "server.keystore";
    private static final String SERVER_TRUST_STORE = "server.truststore";
    private static final char[] STORE_PASSWORD = "password".toCharArray();
    private static XnioWorker worker;
    private static RestClientTemplate restClientTemplate;
    static Pet pet;

    @BeforeClass
    public static void setUp() throws IOException {
        restClientTemplate = new RestClientTemplate();

        final Xnio xnio = Xnio.getInstance();
        final XnioWorker xnioWorker = xnio.createWorker(null, Http2Client.DEFAULT_OPTIONS);
        worker = xnioWorker;

        if(server == null) {
            System.out.println("starting server");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            builder.addHttpsListener(9991, "localhost", sslContext);
            builder.addHttpListener(9990, "localhost");

            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);

            pet = new Pet();
            pet.setId(1L);
            pet.setName("cat");
            pet.setTag("tag1");

            server = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath("/message", exchange -> {
                                exchange.setStatusCode(StatusCodes.OK);
                                exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "Hello".length() + "");
                                final Sender sender = exchange.getResponseSender();
                                sender.send("Hello");
                            })
                            .addExactPath("/api", (exchange) -> {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString("OK"));

                            })
                            .addExactPath("/v1/pets/1", (exchange) -> {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(pet));

                            })
                            .addExactPath("post", exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    try {
                                        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(message));
                                    } catch (Exception e) {

                                    }
                                }
                            })))
                    .setWorkerThreads(200)
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
            logger.info("The router server1 is stopped.");
        }
    }

    @Test
    public void testGet() throws  Exception {
        String str = restClientTemplate.get("http://localhost:9990", "/api", String.class);

        System.out.println(str);
        assertNotNull(str);
    }

    @Test
    public void testGetWithType() throws RestClientException{
        Pet pet = restClientTemplate.get("https://localhost:9991", "/v1/pets/1", Pet.class);
        assertTrue(pet.getId()==1);
    }

    @Ignore
    @Test
    public void testPost() throws RestClientException, JsonProcessingException {
        String requestBody = Config.getInstance().getMapper().writeValueAsString(pet);
        String str = restClientTemplate.post("https://localhost:9991", "post", requestBody);
        assertNotNull(str);
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
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

}
