package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StringUtilsTest {
    //@Test
    public void testExpandEnvVars() {
        String s = "IP=${DOCKER_HOST_IP}";
        Assert.assertEquals("IP=192.168.1.120", StringUtils.expandEnvVars(s));
    }

    @Test
    public void testInputStreamToString_withExpected() throws IOException {
        String expected = "test data";
        InputStream anyInputStream = new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8));
        String actual = StringUtils.inputStreamToString(anyInputStream, StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }
}
