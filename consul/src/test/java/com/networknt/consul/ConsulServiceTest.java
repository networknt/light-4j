package com.networknt.consul;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ConsulServiceTest {
    @Test
    public void testToStringSingleTag() {
        ConsulService service = new ConsulService();
        service.setId("127.0.0.1:com.networknt.apib-1.0.0:7442");
        service.setName("com.networknt.apib-1.0.0");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        service.setTtl(30);
        List tags = new ArrayList();
        tags.add("protocol_light");
        service.setTags(tags);

        String s = service.toString();
        System.out.println("s = " + s);
        Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"DeregisterCriticalServiceAfter\":\"1m\",\"TTL\":\"30s\"}}", s);
    }

    @Test
    public void testToStringMultipleTag() {
        ConsulService service = new ConsulService();
        service.setId("127.0.0.1:com.networknt.apib-1.0.0:7442");
        service.setName("com.networknt.apib-1.0.0");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        service.setTtl(30);
        List tags = new ArrayList();
        tags.add("protocol_light");
        tags.add("second_tag");
        service.setTags(tags);

        String s = service.toString();
        System.out.println("s = " + s);
        Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\",\"second_tag\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"DeregisterCriticalServiceAfter\":\"1m\",\"TTL\":\"30s\"}}", s);
    }

}
