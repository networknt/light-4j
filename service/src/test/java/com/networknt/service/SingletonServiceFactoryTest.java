package com.networknt.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by stevehu on 2016-11-26.
 */
public class SingletonServiceFactoryTest {
    @Test
    public void testGetBean() {
        A a = (A)SingletonServiceFactory.getBean(A.class);
        Assert.assertEquals("a real", a.a());

        B b = (B)SingletonServiceFactory.getBean(B.class);
        Assert.assertEquals("b test", b.b());
    }

}
