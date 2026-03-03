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
import io.undertow.server.HttpHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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


}
