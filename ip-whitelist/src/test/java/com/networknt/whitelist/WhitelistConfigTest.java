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

package com.networknt.whitelist;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.junit.Test;

public class WhitelistConfigTest {
    @Test
    public void testLoadConfig() {
        WhitelistConfig config = (WhitelistConfig) Config.getInstance().getJsonObjectConfig("whitelist", WhitelistConfig.class);
        System.out.println(config);
    }

    @Test
    public void testConfigMapFormat() {
        WhitelistConfig config = WhitelistConfig.load("whitelist-map");
        System.out.println(config);
    }

    @Test
    public void testConfigJsonFormat() {
        WhitelistConfig config = WhitelistConfig.load("whitelist-json");
        System.out.println(config);
    }

    @Test
    public void testComparison() {
        WhitelistConfig configYaml = WhitelistConfig.load("whitelist-map");
        WhitelistConfig configJson = WhitelistConfig.load("whitelist-json");
        System.out.println(JsonMapper.toJson(configYaml));
        System.out.println(JsonMapper.toJson(configJson));


    }
}
