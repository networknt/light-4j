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

import java.util.Map;

public class BasicAuthConfigTest {
    static final Logger logger = LoggerFactory.getLogger(BasicAuthConfigTest.class);
    static final BasicAuthConfig config = BasicAuthConfig.load();

    @Test
    public void testDecryption() {
        logger.debug("password for user2 = " + config.getUsers().get("user2").getPassword());
        Assert.assertEquals("password", config.getUsers().get("user2").getPassword());
    }

    @Test
    public void testPaths() {
        Map<String, UserAuth> users = config.getUsers();
        UserAuth user = users.get("user1");
        Assert.assertEquals("user1", user.getUsername());
        Assert.assertEquals("user1pass", user.getPassword());
        Assert.assertEquals(1, user.getPaths().size());
        Assert.assertEquals("/v1/address", user.getPaths().get(0));
    }
}
