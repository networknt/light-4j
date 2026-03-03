package com.networknt.limit;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
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

public class LimitConfigReloadTest {
    static final Logger logger = LoggerFactory.getLogger(LimitConfigReloadTest.class);
    static Undertow server = null;
    static final String CONFIG_NAME = "limit";
    static byte[] backup;

    @BeforeAll
    public static void setUp() throws Exception {
        Config.getInstance().clear();
        if (server == null) {
            logger.info("starting server");
            // Backup config
            java.nio.file.Path path = java.nio.file.Paths.get("target/test-classes/config/limit.yaml");
            if(java.nio.file.Files.exists(path)) {
                backup = java.nio.file.Files.readAllBytes(path);
            }

            // Load initial config
            Config.getInstance().getMapper().writeValue(
                    path.toFile(),
                    java.util.Map.of(
                            "enabled", true,
                            "concurrentRequest", 1,
                            "queueSize", -1,
                            "errorCode", 429,
                            "key", "server",
                            "rateLimit", "1/s"
                    )
            );

            HttpHandler handler = getTestHandler();
            LimitHandler limitHandler = new LimitHandler();
            limitHandler.setNext(handler);
            handler = limitHandler;
            server = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
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
        // Restore config
        if(backup != null) {
            java.nio.file.Files.write(java.nio.file.Paths.get("target/test-classes/config/limit.yaml"), backup);
        }
        Config.getInstance().clear();
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testLimitReload() throws Exception {
        // 1. Send first request - should success
        Assertions.assertEquals(200, sendRequest());

        // 2. Trigger rate limit (1/s) with concurrent requests
        boolean rateLimited = false;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(10);
        java.util.List<java.util.concurrent.Callable<Integer>> tasks = java.util.Collections.nCopies(10, () -> {
            try {
                int code = sendRequest();
                return code;
            } catch (Exception e) {
                return -1;
            }
        });
        java.util.List<java.util.concurrent.Future<Integer>> futures = executor.invokeAll(tasks);
        for(java.util.concurrent.Future<Integer> f : futures) {
            if(f.get() == 429) {
                rateLimited = true;
                break;
            }
        }
        executor.shutdown();

        Assertions.assertTrue(rateLimited, "Should have triggered rate limit");


        // 3. Update config to allow more requests (100/s)
        Config.getInstance().getMapper().writeValue(
                java.nio.file.Paths.get("target/test-classes/config/limit.yaml").toFile(),
                java.util.Map.of(
                        "enabled", true,
                        "concurrentRequest", 100,
                        "queueSize", -1,
                        "errorCode", 429,
                        "key", "server",
                        "rateLimit", "100/s"
                )
        );

        // 4. Trigger reload
        // Clear config cache to force reload from file
        Config.getInstance().clear();

        // Give a little time for whatever async refresh if any, though reload() is sync.
        Thread.sleep(1100); // Sleep > 1s to start a fresh second for the rate limiter window

        // 5. Send requests again - should success
        // Send multiple requests to verify limit is indeed increased
        for(int i=0; i<5; i++) {
            Assertions.assertEquals(200, sendRequest(), "Request " + i + " should succeed after reload");
        }
    }

    private int sendRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        return reference.get().getResponseCode();
    }
}
