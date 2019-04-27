package com.networknt.client.oauth;

import org.junit.Assert;
import org.junit.Test;

public class SignKeyRequestTest {
    @Test
    public void testConstructor() {
        SignKeyRequest request = new SignKeyRequest("001");
        Assert.assertEquals("001", request.getKid());
        Assert.assertEquals("f7d42348-c647-4efb-a52d-4c5787421e72", request.getClientId());
        Assert.assertEquals("f6h1FTI8Q3-7UScPZDzfXA", request.getClientSecret());
        Assert.assertTrue(request.isEnableHttp2());
    }
}
