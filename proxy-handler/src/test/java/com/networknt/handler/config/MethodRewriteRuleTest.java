package com.networknt.handler.config;

import com.networknt.utility.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MethodRewriteRuleTest {

    @Test
    public void testMethodRewriteRule() {
        MethodRewriteRule methodRewriteRule = new MethodRewriteRule("/gateway/sit/service-request/{tracing-no}", "PUT", "PATCH");
        String requestPath = "/gateway/sit/service-request/123?version=1.1";
        Assertions.assertTrue(StringUtils.matchPathToPattern(requestPath, methodRewriteRule.getRequestPath()));
        requestPath = "/gateway/sit/service-request?version=2";
        Assertions.assertFalse(StringUtils.matchPathToPattern(requestPath, methodRewriteRule.getRequestPath()));
    }
}
