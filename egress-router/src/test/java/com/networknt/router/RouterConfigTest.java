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

import com.networknt.config.JsonMapper;
import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterConfigTest {

    private static RouterConfig routerConfig;

    @BeforeClass
    public static void setUp() {
        routerConfig = RouterConfig.load();
    }


    @Test
    public void testConfig() {
        Assert.assertFalse(routerConfig.isHttp2Enabled());
        Assert.assertTrue(routerConfig.isHttpsEnabled());
        Assert.assertTrue(routerConfig.isRewriteHostHeader());
        Assert.assertEquals(routerConfig.getMaxRequestTime(), 1000);
        Assert.assertEquals(routerConfig.getMaxConnectionRetries(), 3);
    }

    @Test
    public void testConfigList() {
        Assert.assertNotNull(routerConfig.getHostWhitelist());
        Assert.assertEquals(routerConfig.getHostWhitelist().size(), 2);
    }

    @Test
    public void testUrlRewriteRules() {
        Assert.assertNotNull(routerConfig.getUrlRewriteRules());
        Assert.assertEquals(routerConfig.getUrlRewriteRules().size(), 3);
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
        Assert.assertEquals(targetURL, s);
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
        Assert.assertEquals(targetURL, s);
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
        Assert.assertEquals(targetURL, s);
        System.out.println(s);
    }

}
