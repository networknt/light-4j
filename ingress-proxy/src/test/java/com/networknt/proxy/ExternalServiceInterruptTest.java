package com.networknt.proxy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import javax.net.ssl.*;
import static org.junit.jupiter.api.Assertions.*;

class ExternalServiceInterruptTest {
    @ParameterizedTest
    @CsvSource({"1,1", "3,1", "3,3"})
    void interruptionStopsRetriesAndPreservesFlag(int attempts, int interruptAt) throws Exception {
        var supported = ExternalServiceHandler.class.getDeclaredField("connectionCloseHeaderSupported");
        var logged = ExternalServiceHandler.class.getDeclaredField("connectionCloseHeaderFallbackLogged");
        supported.setAccessible(true);
        logged.setAccessible(true);
        Object previousSupported = supported.get(null);
        var loggedFlag = (java.util.concurrent.atomic.AtomicBoolean) logged.get(null);
        boolean previousLogged = loggedFlag.get();
        InterruptingClient client = new InterruptingClient(interruptAt);
        var method = ExternalServiceHandler.class.getDeclaredMethod("sendWithRetries", HttpClient.class, HttpRequest.class, int.class);
        method.setAccessible(true);
        try {
            var thrown = assertThrows(InvocationTargetException.class, () -> method.invoke(
                    new ExternalServiceHandler(), client, HttpRequest.newBuilder(URI.create("http://localhost/")).build(), attempts));
            assertInstanceOf(InterruptedException.class, thrown.getCause());
            assertEquals(interruptAt, client.calls);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            supported.set(null, previousSupported);
            loggedFlag.set(previousLogged);
        }
    }

    private static final class InterruptingClient extends HttpClient {
        private final int interruptAt;
        private int calls;
        InterruptingClient(int interruptAt) { this.interruptAt = interruptAt; }
        @Override public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException, InterruptedException {
            if (++calls == interruptAt) throw new InterruptedException("cancelled");
            throw new IOException("retryable");
        }
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException();
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler,
                HttpResponse.PushPromiseHandler<T> push) { throw new UnsupportedOperationException(); }
    }
}
