package com.networknt.config;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PathPrefixAuthTest {

    /**
     * This test can confirm that even httpClient is added to the class, we can still use jackson to
     * convert a string into an array of PathPrefixAuth objects.
     */
    @Test
    public void testPathPrefixAuth() {
        String s = "[{\"clientId\":\"my-client\",\"clientSecret\":\"my-secret\",\"tokenUrl\":\"www.example.com/token\"}]";
        List<PathPrefixAuth> pathPrefixAuths = null;
        try {
            pathPrefixAuths = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertNotNull(pathPrefixAuths);
        Assertions.assertEquals(pathPrefixAuths.size(), 1);
    }

}
