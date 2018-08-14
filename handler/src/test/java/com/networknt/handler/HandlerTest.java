package com.networknt.handler;

import com.networknt.utility.Tuple;
import io.undertow.server.HttpHandler;
import org.junit.Assert;
import org.junit.Test;

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

    @Test(expected = Exception.class)
    public void invalidMethod_init_throws() throws Exception {
        Handler.setConfig("invalid-method");
    }


}
