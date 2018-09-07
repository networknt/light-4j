package com.networknt.handler;

import com.networknt.handler.config.EndpointSource;
import com.networknt.handler.config.PathChain;
import com.networknt.utility.Tuple;
import io.undertow.server.HttpHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.PathTemplateMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HandlerTest {

    @Test
    public void validClassNameWithoutAt_split_returnsCorrect() throws Exception {
        Tuple<String, Class> sample1 = Handler.splitClassAndName("com.networknt.handler.sample.SampleHttpHandler1");
        Assert.assertEquals("com.networknt.handler.sample.SampleHttpHandler1", sample1.first);
        Assert.assertEquals(Class.forName("com.networknt.handler.sample.SampleHttpHandler1"), sample1.second);
    }

    @Test
    public void validClassNameWithAt_split_returnsCorrect() throws Exception {
        Tuple<String, Class> sample1 = Handler.splitClassAndName("com.networknt.handler.sample.SampleHttpHandler1@Hello");
        Assert.assertEquals("Hello", sample1.first);
        Assert.assertEquals(Class.forName("com.networknt.handler.sample.SampleHttpHandler1"), sample1.second);
    }

    @Test
    public void validConfig_init_handlersCreated() {
    	Handler.init();
        Map<String, List<HttpHandler>> handlers = Handler.handlerListById;
        Assert.assertEquals(1, handlers.get("third").size());
        Assert.assertEquals(2, handlers.get("secondBeforeFirst").size());
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
        Assert.assertNotNull(getFirst);
        PathTemplateMatcher.PathMatchResult<String> getSecond = getMatcher.match("/my-api/second");
        Assert.assertNotNull(getSecond);
        PathTemplateMatcher.PathMatchResult<String> getThird = getMatcher.match("/my-api/third");
        Assert.assertNull(getThird);
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
            Assert.fail("Expected an exception to be thrown");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Conflicting source"));
            Assert.assertTrue(e.getMessage().contains("and path"));
            Assert.assertTrue(e.getMessage().contains("and method"));
        }
    }

    @Test(expected = Exception.class)
    public void invalidMethod_init_throws() throws Exception {
        Handler.setConfig("invalid-method");
    }


}
