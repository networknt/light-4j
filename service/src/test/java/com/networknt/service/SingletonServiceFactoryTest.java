package com.networknt.service;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by stevehu on 2016-11-26.
 */
public class SingletonServiceFactoryTest {
    @Test
    public void testGetSingleBean() {
        A a = (A)SingletonServiceFactory.getBean(A.class);
        Assert.assertEquals("a real", a.a());

        B b = (B)SingletonServiceFactory.getBean(B.class);
        Assert.assertEquals("b test", b.b());

        C c = (C)SingletonServiceFactory.getBean(C.class);
        Assert.assertEquals("a realb test", c.c());
    }

    @Test
    public void testGetMultipleBean() {
        Object object = SingletonServiceFactory.getBean(Processor.class);
        //Processor[] processors = (Processor[])object;
        Arrays.stream((Processor[])object).forEach(processor -> System.out.println(processor.process()));
    }
}
