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
package com.networknt.registry.support.command;

import com.networknt.registry.URL;
import com.networknt.registry.support.DirectRegistryConfig;
import com.networknt.utility.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DirectRegistryConfigTest {
    @Test
    public void blankDirectUrlsLoadsAsEmptyMap() {
        DirectRegistryConfig config = DirectRegistryConfig.load("direct-registry-blank");

        Assertions.assertTrue(config.getDirectUrls().isEmpty());
    }

    @Test
    public void directUrlWithoutPathHasNoBasePath() {
        DirectRegistryConfig config = DirectRegistryConfig.load();

        URL url = config.getDirectUrls().get("token").get(0);
        Assertions.assertEquals("192.168.1.100", url.getHost());
        Assertions.assertNull(url.getParameter(Constants.BASE_PATH));
    }

    @Test
    public void directUrlWithPathMovesItToBasePath() {
        DirectRegistryConfig config = DirectRegistryConfig.load();

        URL url = config.getDirectUrls().get("com.networknt.ingress-1.0.0").get(0);
        Assertions.assertEquals("api.example.com", url.getHost());
        Assertions.assertEquals(443, url.getPort());
        Assertions.assertEquals("/namespace1/service1", url.getParameter(Constants.BASE_PATH));
        Assertions.assertTrue(url.getPath() == null || url.getPath().isEmpty());
    }

    @Test
    public void directUrlWithDelimiterInPathKeepsTheWholeBasePath() {
        DirectRegistryConfig config = DirectRegistryConfig.load();

        URL url = config.getDirectUrls().get("com.networknt.delimiter-1.0.0").get(0);
        Assertions.assertEquals("api.example.com", url.getHost());
        Assertions.assertEquals("/namespace3/a&b", url.getParameter(Constants.BASE_PATH));
    }

    @Test
    public void directUrlWithPathAndEnvironmentKeepsBothParameters() {
        DirectRegistryConfig config = DirectRegistryConfig.load();

        URL url = config.getDirectUrls().get("ingress|0000").get(0);
        Assertions.assertEquals("/namespace1/service1", url.getParameter(Constants.BASE_PATH));
        Assertions.assertEquals("0000", url.getParameter(Constants.TAG_ENVIRONMENT));
    }

    @Test
    public void missingDirectUrlsLoadsAsEmptyMap() {
        DirectRegistryConfig config = DirectRegistryConfig.load("direct-registry-missing");

        Assertions.assertTrue(config.getDirectUrls().isEmpty());
    }
}
