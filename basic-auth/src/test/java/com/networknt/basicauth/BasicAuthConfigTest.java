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

package com.networknt.basicauth;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuthConfigTest {
    static final Logger logger = LoggerFactory.getLogger(BasicAuthConfigTest.class);

    static final String CONFIG_NAME = "basic-auth";
    static final BasicAuthConfig config = (BasicAuthConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BasicAuthConfig.class);

    @Test
    public void testDecryption() {
        logger.debug("password for user2 = " + config.getUsers().get(1).get("password"));
        Assert.assertEquals("password", config.getUsers().get(1).get("password"));
    }
}
