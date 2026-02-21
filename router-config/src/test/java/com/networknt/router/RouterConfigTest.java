/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterConfigTest {

    private static RouterConfig routerConfig;

    @BeforeAll
    public static void setUp() {
        routerConfig = RouterConfig.load();
    }


    @Test
    public void testConfig() {
        Assertions.assertFalse(routerConfig.isHttp2Enabled());
        Assertions.assertTrue(routerConfig.isHttpsEnabled());
        Assertions.assertTrue(routerConfig.isRewriteHostHeader());
        Assertions.assertEquals(routerConfig.getMaxRequestTime(), 1000);
        Assertions.assertEquals(routerConfig.getMaxConnectionRetries(), 3);
        Assertions.assertEquals(routerConfig.getMaxQueueSize(), 0);
    }

    @Test
    public void testConfigList() {
        Assertions.assertNotNull(routerConfig.getHostWhitelist());
        Assertions.assertEquals(routerConfig.getHostWhitelist().size(), 2);
    }

    @Test
    public void testQueryParamRewriteRules() {
        Assertions.assertNotNull(routerConfig.getQueryParamRewriteRules());
        Assertions.assertEquals(routerConfig.getQueryParamRewriteRules().size(), 4);
    }

    @Test
    public void testHeaderRewriteRules() {
        Assertions.assertNotNull(routerConfig.getHeaderRewriteRules());
        Assertions.assertEquals(routerConfig.getHeaderRewriteRules().size(), 4);
    }


    @Test
    public void testUrlRewriteRules() {
        Assertions.assertNotNull(routerConfig.getUrlRewriteRules());
        Assertions.assertEquals(routerConfig.getUrlRewriteRules().size(), 3);
    }

    @Test
    public void testRegexReplace1() {
        String sourceURL = "/listings/123";
        String targetURL = "/listing.html?listing=123";
        Pattern pattern = Pattern.compile("/listings/(.*)$");
        String replace = "/listing.html?listing=$1";

        Matcher matcher = pattern.matcher(sourceURL);
        String s = null;
        if(matcher.matches()) {
            s = matcher.replaceAll(replace);
        }
        Assertions.assertEquals(targetURL, s);
        System.out.println(s);
    }

    @Test
    public void testRegexReplace2() {
        String sourceURL = "/ph/uat/de-asia-ekyc-service/v1";
        String targetURL = "/uat-de-asia-ekyc-service/v1";
        Pattern pattern = Pattern.compile("/ph/uat/de-asia-ekyc-service/v1");
        String replace = "/uat-de-asia-ekyc-service/v1";

        Matcher matcher = pattern.matcher(sourceURL);
        String s = matcher.replaceAll(replace);
        Assertions.assertEquals(targetURL, s);
        System.out.println(s);
    }

    @Test
    public void testRegexReplace3() {
        String sourceURL = "/tutorial/linux/wordpress/file1";
        String targetURL = "/tutorial/linux/cms/file1.php";
        Pattern pattern = Pattern.compile("(/tutorial/.*)/wordpress/(\\w+)\\.?.*$");
        String replace = "$1/cms/$2.php";

        Matcher matcher = pattern.matcher(sourceURL);
        String s = matcher.replaceAll(replace);
        Assertions.assertEquals(targetURL, s);
        System.out.println(s);
    }

    @Test
    public void testPathPrefixMaxRequestTime() {
        Map<String, Integer> map = routerConfig.getPathPrefixMaxRequestTime();
        System.out.println("map size = " + map.size());
    }
}
