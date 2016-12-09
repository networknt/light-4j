package com.networknt.service;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by steve on 2016-11-26.
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

    @Test
    public void testMultipleInterfaceOneBean() {
        Object object1 = SingletonServiceFactory.getBean(D1.class);
        Object object2 = SingletonServiceFactory.getBean(D2.class);
        Assert.assertEquals(object1, object2);
    }

    @Test
    public void testMultipleToMultiple() {
        Object e = SingletonServiceFactory.getBean(E.class);
        Arrays.stream((E[])e).forEach(o -> System.out.println(o.e()));
        Object f = SingletonServiceFactory.getBean(F.class);
        Arrays.stream((F[])f).forEach(o -> System.out.println(o.f()));
    }

    @Test
    public void testSingleWithProperties() {
        G g = (G)SingletonServiceFactory.getBean(G.class);
        Assert.assertEquals("Sky Walker", g.getName());
        Assert.assertEquals(23, g.getAge());

    }

    //@Test
    public void testMultipleWithProperties() {
        Object j = SingletonServiceFactory.getBean(J.class);
        Arrays.stream((J[])j).forEach(o -> System.out.println(o.getJack()));
        Object k = SingletonServiceFactory.getBean(K.class);
        Arrays.stream((K[])k).forEach(o -> System.out.println(o.getKing()));

    }
}
