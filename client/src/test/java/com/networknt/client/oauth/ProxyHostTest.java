package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * All test cases here are marked ignored as the proxyHost and proxyPort in the token section
 * is commented out by default. We only run these tests with updated client.yml in the test
 * folder during the development and debugging.
 *
 * @author Steve Hu
 */

public class ProxyHostTest {
    public static final String CONFIG_NAME = "client-proxy";
    static ClientConfig config;

    @BeforeAll
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
    }

    @Test
    public void testKeyRequest() {
        TokenKeyRequest request = new TokenKeyRequest("001");
        Assertions.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assertions.assertEquals(3128, request.getProxyPort());
    }

    @Test
    public void testClientCredentialsRequest() {
        ClientCredentialsRequest request = new ClientCredentialsRequest(null);
        Assertions.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assertions.assertEquals(3128, request.getProxyPort());
    }

    @Test
    public void testAuthorizationCodeRequest() {
        AuthorizationCodeRequest request = new AuthorizationCodeRequest();
        Assertions.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assertions.assertEquals(3128, request.getProxyPort());
    }

    @Test
    public void testClientAuthenticatedUserRequest() {
        ClientAuthenticatedUserRequest request = new ClientAuthenticatedUserRequest("Employee", "userId", "admin");
        Assertions.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assertions.assertEquals(3128, request.getProxyPort());
    }

}
