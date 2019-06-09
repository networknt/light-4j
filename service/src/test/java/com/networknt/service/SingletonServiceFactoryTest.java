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

package com.networknt.service;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by steve on 2016-11-26.
 */
public class SingletonServiceFactoryTest {
    private static Logger logger = LoggerFactory.getLogger(SingletonServiceFactoryTest.class);

    @BeforeClass
    public static void setup() {
        InjectedBean injectedBean = new InjectedBean();
        SingletonServiceFactory.setBean(InjectedBean.class.getName(), injectedBean);
    }

    @Test
    public void testInjectedBean() {
        InjectedBean injectedBean = SingletonServiceFactory.getBean(InjectedBean.class);
        Assert.assertEquals("Injected Bean", injectedBean.name());
    }

    @Test
    public void testGetSingleBean() {
        A a = SingletonServiceFactory.getBean(A.class);
        Assert.assertEquals("a real", a.a());

        B b = SingletonServiceFactory.getBean(B.class);
        Assert.assertEquals("b test", b.b());

        C c = SingletonServiceFactory.getBean(C.class);
        Assert.assertEquals("a realb test", c.c());
    }

    @Test
    public void testGetMultipleBean() {
        Processor[] processors = SingletonServiceFactory.getBeans(Processor.class);
        Assert.assertEquals(processors.length, 3);
        Arrays.stream(processors).forEach(processor -> logger.debug(processor.process()));
    }

    @Test
    public void testMultipleInterfaceOneBean() {
        D1 d1 = SingletonServiceFactory.getBean(D1.class);
        D2 d2 = SingletonServiceFactory.getBean(D2.class);
        Assert.assertEquals(d1, d2);
    }

    @Test
    public void testMultipleToMultiple() {
        E[] e = SingletonServiceFactory.getBeans(E.class);
        Arrays.stream(e).forEach(o -> logger.debug(o.e()));
        F[] f = SingletonServiceFactory.getBeans(F.class);
        Arrays.stream(f).forEach(o -> logger.debug(o.f()));
    }

    @Test
    public void testSingleWithProperties() {
        G g = SingletonServiceFactory.getBean(G.class);
        Assert.assertEquals("Sky Walker", g.getName());
        Assert.assertEquals(23, g.getAge());

    }

    @Test
    @Ignore
    public void testMultipleWithProperties() {
        J[] j = SingletonServiceFactory.getBeans(J.class);
        Arrays.stream(j).forEach(o -> logger.debug(o.getJack()));
        K[] k = SingletonServiceFactory.getBeans(K.class);
        Arrays.stream(k).forEach(o -> logger.debug(o.getKing()));

    }

    @Test
    public void testMap() {
        LImpl l = (LImpl)SingletonServiceFactory.getBean(L.class);
        Assert.assertEquals("https", l.getProtocol());
        Assert.assertEquals(8080, l.getPort());
        Assert.assertEquals(2, l.getParameters().size());
    }

    @Test
    public void testConstructorWithParameters() {
        MImpl m = (MImpl)SingletonServiceFactory.getBean(M.class);
        Assert.assertEquals(5, m.getValue());
    }

    @Test
    public void testInfo() {
        Info info = SingletonServiceFactory.getBean(Info.class);
        Assert.assertEquals("contact", info.getContact().getName());
        Assert.assertEquals("license", info.getLicense().getName());
    }

    @Test
    public void testInfoValidator() {
        Validator<Info> infoValidator = SingletonServiceFactory.getBean(Validator.class, Info.class);
        Info info = SingletonServiceFactory.getBean(Info.class);
        Assert.assertTrue(infoValidator.validate(info));
    }

    @Test
    public void testArrayFromSingle() {
        // get an array with only one implementation.
        A[] a = SingletonServiceFactory.getBeans(A.class);
        Assert.assertEquals(1, a.length);
    }

    @Test
    public void testSingleFromArray() {
        // get the first object from an array of impelementation in service.yml
        E e = SingletonServiceFactory.getBean(E.class);
        Assert.assertEquals("e1", e.e());
    }

    @Test
    public void testArrayNotDefined() {
        Dummy[] dummies = SingletonServiceFactory.getBeans(Dummy.class);
        Assert.assertNull(dummies);
    }

    @Test
    public void testObjectNotDefined() {
        Dummy dummy = SingletonServiceFactory.getBean(Dummy.class);
        Assert.assertNull(dummy);
    }

    @Test
    public void testInitializerInterfaceWithBuilder() {
        ChannelMapping channelMapping = SingletonServiceFactory.getBean(ChannelMapping.class);
        Assert.assertNotNull(channelMapping);
        Assert.assertTrue(channelMapping.transform("ReplyTo").startsWith("aggregate-destination-"));
    }


}
