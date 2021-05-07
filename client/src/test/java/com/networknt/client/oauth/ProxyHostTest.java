package com.networknt.client.oauth;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * All test cases here are marked ignored as the proxyHost and proxyPort in the token section
 * is commented out by default. We only run these tests with updated client.yml in the test
 * folder during the development and debugging.
 *
 * @author Steve Hu
 */

public class ProxyHostTest {
    @Test
    @Ignore
    public void testKeyRequest() {
        TokenKeyRequest request = new TokenKeyRequest("001");
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

    @Test
    @Ignore
    public void testClientCredentialsRequest() {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

    @Test
    @Ignore
    public void testAuthorizationCodeRequest() {
        AuthorizationCodeRequest request = new AuthorizationCodeRequest();
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

    @Test
    @Ignore
    public void testClientAuthenticatedUserRequest() {
        ClientAuthenticatedUserRequest request = new ClientAuthenticatedUserRequest("Employee", "userId", "admin");
        Assert.assertEquals("proxy.lightapi.net", request.getProxyHost());
        Assert.assertEquals(3128, request.getProxyPort());
    }

}
