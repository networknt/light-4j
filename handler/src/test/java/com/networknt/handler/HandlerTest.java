package com.networknt.handler;

import com.networknt.common.Tuple;
import org.junit.Assert;
import org.junit.Test;

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


    // Test handlers are initialized properly

    // Test invalid verbs


}
