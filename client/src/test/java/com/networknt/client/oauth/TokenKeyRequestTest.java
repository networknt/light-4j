package com.networknt.client.oauth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenKeyRequestTest {
    @Test
    public void testConstructor() {
        SignKeyRequest request = new SignKeyRequest("001");
        Assertions.assertEquals("001", request.getKid());
        Assertions.assertEquals("f7d42348-c647-4efb-a52d-4c5787421e72", request.getClientId());
        Assertions.assertEquals("f6h1FTI8Q3-7UScPZDzfXA", request.getClientSecret());
        Assertions.assertTrue(request.isEnableHttp2());
    }
}
