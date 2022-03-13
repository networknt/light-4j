package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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

    @BeforeClass
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
    }

    @Test
    public void testKeyRequest() {
        TokenKeyRequest request = new TokenKeyRequest("001");
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

    @Test
    public void testClientCredentialsRequest() {
        ClientCredentialsRequest request = new ClientCredentialsRequest(null);
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

    @Test
    public void testAuthorizationCodeRequest() {
        AuthorizationCodeRequest request = new AuthorizationCodeRequest();
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

    @Test
    public void testClientAuthenticatedUserRequest() {
        ClientAuthenticatedUserRequest request = new ClientAuthenticatedUserRequest("Employee", "userId", "admin");
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

}
