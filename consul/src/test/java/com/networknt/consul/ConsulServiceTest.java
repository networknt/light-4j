package com.networknt.consul;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.networknt.consul.ConsulConstants.CONFIG_NAME;

public class ConsulServiceTest {
    static ConsulConfig config = (ConsulConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ConsulConfig.class);
    @Test
    public void testToStringSingleTag() {
        ConsulService service = new ConsulService();
        service.setId("127.0.0.1:com.networknt.apib-1.0.0:7442");
        service.setName("com.networknt.apib-1.0.0");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        List tags = new ArrayList();
        tags.add("protocol_light");
        service.setTags(tags);

        String s = service.toString();
        System.out.println("s = " + s);
        if(config.tcpCheck) {
            Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"ID\":\"check-127.0.0.1:com.networknt.apib-1.0.0:7442\",\"DeregisterCriticalServiceAfter\":\"90m\",\"TCP\":\"127.0.0.1:7442\",\"Interval\":\"10s\"}}", s);
        } else if(config.httpCheck) {
            Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"ID\":\"check-127.0.0.1:com.networknt.apib-1.0.0:7442\",\"DeregisterCriticalServiceAfter\":\"90m\",\"HTTP\":\"https://127.0.0.1:7442/health/com.networknt.apib-1.0.0\",\"TLSSkipVerify\":true,\"Interval\":\"10s\"}}", s);
        } else {
            Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"ID\":\"check-127.0.0.1:com.networknt.apib-1.0.0:7442\",\"DeregisterCriticalServiceAfter\":\"90m\",\"TTL\":\"10s\"}}", s);
        }

    }

    @Test
    public void testToStringMultipleTag() {
        ConsulService service = new ConsulService();
        service.setId("127.0.0.1:com.networknt.apib-1.0.0:7442");
        service.setName("com.networknt.apib-1.0.0");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        List tags = new ArrayList();
        tags.add("protocol_light");
        tags.add("second_tag");
        service.setTags(tags);

        String s = service.toString();
        System.out.println("s = " + s);
        if(config.tcpCheck) {
            Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\",\"second_tag\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"ID\":\"check-127.0.0.1:com.networknt.apib-1.0.0:7442\",\"DeregisterCriticalServiceAfter\":\"90m\",\"TCP\":\"127.0.0.1:7442\",\"Interval\":\"10s\"}}", s);
        } else if(config.httpCheck) {
            Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\",\"second_tag\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"ID\":\"check-127.0.0.1:com.networknt.apib-1.0.0:7442\",\"DeregisterCriticalServiceAfter\":\"90m\",\"HTTP\":\"https://127.0.0.1:7442/health/com.networknt.apib-1.0.0\",\"TLSSkipVerify\":true,\"Interval\":\"10s\"}}", s);
        } else {
            Assert.assertEquals("{\"ID\":\"127.0.0.1:com.networknt.apib-1.0.0:7442\",\"Name\":\"com.networknt.apib-1.0.0\",\"Tags\":[\"protocol_light\",\"second_tag\"],\"Address\":\"127.0.0.1\",\"Port\":7442,\"Check\":{\"ID\":\"check-127.0.0.1:com.networknt.apib-1.0.0:7442\",\"DeregisterCriticalServiceAfter\":\"90m\",\"TTL\":\"10s\"}}", s);
        }

    }

}
