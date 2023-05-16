package com.networknt.client.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.JsonMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

public class TokenInfoTest {
    /**
     * make sure the JSON returned by the introspection endpoint can be converted to the TokenInfo object
     * even the iat and exp is in string format for some servers.
     *
     */
    @Test
    public void testJson2Object() throws Exception {
       String json = "{  \"active\": true,\"client_id\": \"bb8293f6-ceef-4e7a-90c8-1492e97df19f\",\"token_type\": \"refresh_token\",\"scope\": \"openid profile\",\"sub\": \"cn=odicuser,dc=example,dc=com\",\"exp\": \"86400\",\"iat\": \"1506513918\",\"iss\": \"https://wa.example.com\" }";
       ObjectMapper mapper = new ObjectMapper();
       TokenInfo tokenInfo = mapper.readValue(json, TokenInfo.class);
       System.out.println("tokenInfo = " + tokenInfo);
       tokenInfo = JsonMapper.fromJson(json, TokenInfo.class);
       System.out.println("tokenInfo = " + tokenInfo);
    }

    @Test
    public void testError2Object() throws Exception {
        String json = "{\"error\":\"invalid_request\", \"error_description\": \"token is missing in the request\"}";
        ObjectMapper mapper = new ObjectMapper();
        TokenInfo tokenInfo = mapper.readValue(json, TokenInfo.class);
        System.out.println("tokenInfo = " + tokenInfo);
    }

    @Test
    public void testPropertyGet() throws Exception {
        String json = "{  \"active\": true,\"client_id\": \"bb8293f6-ceef-4e7a-90c8-1492e97df19f\",\"token_type\": \"refresh_token\",\"scope\": \"openid profile\",\"sub\": \"cn=odicuser,dc=example,dc=com\",\"exp\": \"86400\",\"iat\": \"1506513918\",\"iss\": \"https://wa.example.com\" }";
        ObjectMapper mapper = new ObjectMapper();
        TokenInfo object = mapper.readValue(json, TokenInfo.class);

        Field field = object.getClass().getDeclaredField("clientId");
        field.setAccessible(true);
        Object value = field.get(object);
        Assert.assertEquals("bb8293f6-ceef-4e7a-90c8-1492e97df19f", value);
        System.out.println("value = " + value);
    }
}
