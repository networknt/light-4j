package com.networknt.handler.config;

import com.networknt.utility.StringUtils;
import org.junit.Assert;
import org.junit.Test;


public class MethodRewriteRuleTest {

    @Test
    public void testMethodRewriteRule() {
        MethodRewriteRule methodRewriteRule = new MethodRewriteRule("/gateway/sit/service-request/{tracing-no}", "PUT", "PATCH");
        String requestPath = "/gateway/sit/service-request/123?version=1.1";
        Assert.assertTrue(StringUtils.matchPathToPattern(requestPath, methodRewriteRule.getRequestPath()));
        requestPath = "/gateway/sit/service-request?version=2";
        Assert.assertFalse(StringUtils.matchPathToPattern(requestPath, methodRewriteRule.getRequestPath()));
    }
}
