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

package com.networknt.handler;

import com.networknt.handler.config.EndpointSource;
import com.networknt.handler.config.PathChain;
import com.networknt.utility.PathTemplateMatcher;
import com.networknt.utility.Tuple;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HandlerTest {

    @BeforeEach
    public void setUp() throws Exception {
        // Resetting HandlerConfig to default Config before each test run
        Handler.setConfig("handler");
    }

    @Test
    public void validClassNameWithoutAt_split_returnsCorrect() throws Exception {
        Tuple<String, Class> sample1 = Handler.splitClassAndName("com.networknt.handler.sample.SampleHttpHandler1");
        Assertions.assertEquals("com.networknt.handler.sample.SampleHttpHandler1", sample1.first);
        Assertions.assertEquals(Class.forName("com.networknt.handler.sample.SampleHttpHandler1"), sample1.second);
    }

    @Test
    public void validClassNameWithAt_split_returnsCorrect() throws Exception {
        Tuple<String, Class> sample1 = Handler.splitClassAndName("com.networknt.handler.sample.SampleHttpHandler1@Hello");
        Assertions.assertEquals("Hello", sample1.first);
        Assertions.assertEquals(Class.forName("com.networknt.handler.sample.SampleHttpHandler1"), sample1.second);
    }

    @Test
    public void validConfig_init_handlersCreated() {
    	Handler.init();
        Map<String, List<HttpHandler>> handlers = Handler.handlerListById;
        Assertions.assertEquals(1, handlers.get("third").size());
        Assertions.assertEquals(2, handlers.get("secondBeforeFirst").size());
    }

    @Test
    public void requestInjectionUsesTheCurrentHandlerYmlChainForEveryRequest() throws Exception {
        ChainACounter.calls.set(0);
        ChainBCounter.calls.set(0);
        Handler.setConfig("handler-request-injection-chains");
        Undertow server = Undertow.builder()
                .addHttpListener(0, "127.0.0.1")
                .setHandler(new OrchestrationHandler())
                .build();
        server.start();
        try {
            InetSocketAddress address = (InetSocketAddress) server.getListenerInfo().get(0).getAddress();
            URI base = URI.create("http://127.0.0.1:" + address.getPort());
            HttpClient client = HttpClient.newHttpClient();
            Assertions.assertEquals(200, get(client, base.resolve("/chain-a")).statusCode());
            Assertions.assertEquals(200, get(client, base.resolve("/chain-b")).statusCode());
            Assertions.assertEquals(200, get(client, base.resolve("/chain-a")).statusCode());
            Assertions.assertEquals(2, ChainACounter.calls.get());
            Assertions.assertEquals(1, ChainBCounter.calls.get());
        } finally {
            server.stop();
        }
    }

    @Test
    public void chainValidationSeesMaterializedNonDefaultConfigAfterDisabledHandlersAreRemoved() throws Exception {
        ChainValidationProbe.validations.set(0);
        Handler.setConfig("handler-materialized-validation");
        Assertions.assertEquals(1, ChainValidationProbe.validations.get());
    }

    private HttpResponse<Void> get(HttpClient client, URI uri) throws Exception {
        return client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.discarding());
    }

    public static final class ChainACounter implements HttpHandler {
        static final AtomicInteger calls = new AtomicInteger();
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            calls.incrementAndGet();
            Handler.next(exchange);
        }
    }

    public static final class ChainBCounter implements HttpHandler {
        static final AtomicInteger calls = new AtomicInteger();
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            calls.incrementAndGet();
            Handler.next(exchange);
        }
    }

    public static final class TerminalHandler implements HttpHandler {
        @Override public void handleRequest(HttpServerExchange exchange) {
            exchange.getResponseSender().send("done");
        }
    }

    public static final class ChainValidationProbe implements MiddlewareHandler, HandlerChainValidator {
        static final AtomicInteger validations = new AtomicInteger();
        @Override public void validateChain(List<HttpHandler> chain, int index, String location) {
            Assertions.assertEquals("path POST /validated", location);
            Assertions.assertEquals(TerminalHandler.class, chain.get(index + 1).getClass());
            validations.incrementAndGet();
        }
        @Override public void handleRequest(HttpServerExchange exchange) { }
        @Override public HttpHandler getNext() { return null; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
        @Override public boolean isEnabled() { return true; }
    }

    public static final class DisabledMiddleware implements MiddlewareHandler {
        @Override public void handleRequest(HttpServerExchange exchange) { }
        @Override public HttpHandler getNext() { return null; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
        @Override public boolean isEnabled() { return false; }
    }

    private PathChain mkPathChain(String source, String path, String method, String... exec) {
        PathChain pc = new PathChain();
        pc.setSource(source);
        pc.setPath(path);
        pc.setMethod(method);
        pc.setExec(Arrays.asList(exec));
        return pc;
    }

    static class MockEndpointSource implements EndpointSource {

        @Override
        public Iterable<EndpointSource.Endpoint> listEndpoints() {
            return Arrays.asList(
                new EndpointSource.Endpoint("/my-api/first", "get"),
                new EndpointSource.Endpoint("/my-api/first", "put"),
                new EndpointSource.Endpoint("/my-api/second", "get")
            );
        }
    }

    @Test
    public void mixedPathsAndSource() {
        Handler.config.setPaths(Arrays.asList(
            mkPathChain(null, "/my-api/first", "post", "third"),
            mkPathChain(MockEndpointSource.class.getName(), null, null, "secondBeforeFirst", "third"),
            mkPathChain(null, "/my-api/second", "put", "third")
        ));
        Handler.init();

        Map<HttpString, PathTemplateMatcher<String>> methodToMatcher = Handler.methodToMatcherMap;

        PathTemplateMatcher<String> getMatcher = methodToMatcher.get(Methods.GET);
        PathTemplateMatcher.PathMatchResult<String> getFirst = getMatcher.match("/my-api/first");
        Assertions.assertNotNull(getFirst);
        PathTemplateMatcher.PathMatchResult<String> getSecond = getMatcher.match("/my-api/second");
        Assertions.assertNotNull(getSecond);
        PathTemplateMatcher.PathMatchResult<String> getThird = getMatcher.match("/my-api/third");
        Assertions.assertNull(getThird);
    }

    @Test
    public void conflictingSourceAndPath_init_throws() {
        // Reconfigure path chain with an invalid path in the middle
        Handler.config.setPaths(Arrays.asList(
            mkPathChain(null, "/a/good/path", "POST", "third"),
            mkPathChain("source", "/conflicting/path", "PUT", "third"),
            mkPathChain("source", null, null, "some-chain", "third")
        ));

        // Use expectThrows when porting to java5.
        // Not adding(exception=) to @Test since we care _where_ the exception is thrown
        try {
            Handler.init();
            Assertions.fail("Expected an exception to be thrown");
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("Conflicting source"));
            Assertions.assertTrue(e.getMessage().contains("and path"));
            Assertions.assertTrue(e.getMessage().contains("and method"));
        }
    }

    @Test
    public void invalidMethod_init_throws() throws Exception {
        Assertions.assertThrows(Exception.class, () -> {
            Handler.setConfig("invalid-method");
        });
    }

    @Test
    public void checkHandlerMetrics() throws InterruptedException {
        Handler.config.setHandlerMetricsLogLevel("INFO");
        Handler.HandlerMetricsCollector collector = new Handler.HandlerMetricsCollector();
        collector.initNextHandlerMeasurement("handler1");
        Thread.sleep(20);
        collector.initNextHandlerMeasurement("handler2");
        Thread.sleep(25);
        final var report = collector.finalizeHandlerMetrics();
        Assertions.assertTrue(report.contains("handler1"));
        Assertions.assertTrue(report.contains("handler2"));
    }

    @Test
    public void trailingSlashInRequestPath_matchPathWithoutTheFallback_isNotMatched() {
        PathTemplateMatcher<String> getMatcher = Handler.methodToMatcherMap.get(Methods.GET);
        Assertions.assertNotNull(getMatcher);

        // the fallback is opt in, so the chain a request resolves to does not change on upgrade
        Assertions.assertFalse(Handler.config.isTrailingSlashFallback());
        Assertions.assertNull(Handler.matchPath(getMatcher, exchange("/test/")));
    }

    @Test
    public void trailingSlashInRequestPath_matchPath_matchesConfiguredPathAndNormalizesTheExchange() {
        PathTemplateMatcher<String> getMatcher = Handler.methodToMatcherMap.get(Methods.GET);
        Assertions.assertNotNull(getMatcher);

        // on its own the matcher only knows about the exact path configured in handler.yml
        Assertions.assertNotNull(getMatcher.match("/test"));
        Assertions.assertNull(getMatcher.match("/test//"));

        Handler.config.setTrailingSlashFallback(true);

        try {

            // the handler resolves the same chain when the consumer adds trailing slashes
            HttpServerExchange ex = exchange("/test//");
            PathTemplateMatcher.PathMatchResult<String> matched = Handler.matchPath(getMatcher, ex);
            Assertions.assertNotNull(matched);
            Assertions.assertEquals("/test", matched.getMatchedTemplate());
            Assertions.assertEquals(Handler.matchPath(getMatcher, exchange("/test")).getValue(), matched.getValue());

            // the chain and the path that is forwarded downstream have to agree with each other
            Assertions.assertEquals("/test", ex.getRequestPath());
            Assertions.assertEquals("/test", ex.getRelativePath());
            Assertions.assertEquals("/test", ex.getRequestURI());

            // a path that is not configured at all is still unmatched
            Assertions.assertNull(Handler.matchPath(getMatcher, exchange("/not-configured/")));

        } finally {
            Handler.config.setTrailingSlashFallback(false);
        }
    }

    @Test
    public void trimTrailingSlashes_returnsNullWhenThereIsNothingToTrim() {
        Assertions.assertEquals("/test", Handler.trimTrailingSlashes("/test/"));
        Assertions.assertEquals("/test", Handler.trimTrailingSlashes("/test///"));
        Assertions.assertNull(Handler.trimTrailingSlashes("/test"));
        Assertions.assertNull(Handler.trimTrailingSlashes("/"));
        Assertions.assertNull(Handler.trimTrailingSlashes(""));
        Assertions.assertNull(Handler.trimTrailingSlashes(null));
    }

    @Test
    public void encodedTrailingSlashInRequestURI_matchPath_skipsTheFallback() {
        PathTemplateMatcher<String> getMatcher = Handler.methodToMatcherMap.get(Methods.GET);
        Assertions.assertNotNull(getMatcher);

        Handler.config.setTrailingSlashFallback(true);

        try {

            // Undertow decodes the request path but not the request URI, so the trailing slash of
            // the path is not a trailing slash of the URI here. The fallback is skipped instead of
            // resolving a chain for a path that is not the one forwarded downstream.
            HttpServerExchange ex = exchange("/test/");
            ex.setRequestURI("/test%2F");
            Assertions.assertNull(Handler.matchPath(getMatcher, ex));
            Assertions.assertEquals("/test/", ex.getRequestPath());
            Assertions.assertEquals("/test%2F", ex.getRequestURI());

            // the path parameters of the URI are kept and the slashes in front of them are trimmed
            ex = exchange("/test/");
            ex.setRequestURI("/test/;x=1");
            Assertions.assertNotNull(Handler.matchPath(getMatcher, ex));
            Assertions.assertEquals("/test", ex.getRequestPath());
            Assertions.assertEquals("/test;x=1", ex.getRequestURI());

        } finally {
            Handler.config.setTrailingSlashFallback(false);
        }
    }

    @Test
    public void trimTrailingSlashesFromURI_trimsThePathPartOfTheRawURI() {
        Assertions.assertEquals("/test", Handler.trimTrailingSlashesFromURI("/test/", 1));
        Assertions.assertEquals("/test", Handler.trimTrailingSlashesFromURI("/test///", 3));
        Assertions.assertEquals("/test;x=1", Handler.trimTrailingSlashesFromURI("/test//;x=1", 2));
        Assertions.assertEquals("http://localhost/test", Handler.trimTrailingSlashesFromURI("http://localhost/test/", 1));
        Assertions.assertNull(Handler.trimTrailingSlashesFromURI("/test%2F", 1));
        Assertions.assertNull(Handler.trimTrailingSlashesFromURI("/test", 1));
        Assertions.assertNull(Handler.trimTrailingSlashesFromURI("/test/", 0));
        Assertions.assertNull(Handler.trimTrailingSlashesFromURI(null, 1));
    }

    private HttpServerExchange exchange(String requestPath) {
        HttpServerExchange ex = new HttpServerExchange(null);
        ex.setRequestMethod(Methods.GET);
        ex.setRequestPath(requestPath);
        ex.setRelativePath(requestPath);
        ex.setRequestURI(requestPath);
        return ex;
    }


}
