package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilTest {
    //@Test
    public void testExpandEnvVars() {
        String s = "IP=${DOCKER_HOST_IP}";
        Assert.assertEquals("IP=192.168.1.120", StringUtil.expandEnvVars(s));
    }
}
