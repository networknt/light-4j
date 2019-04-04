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

package com.networknt.resource;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class VirtualHostConfigTest {
    @Test
    public void testLoadConfig() {
        VirtualHostConfig config = (VirtualHostConfig) Config.getInstance().getJsonObjectConfig(VirtualHostConfig.CONFIG_NAME, VirtualHostConfig.class);
        Assert.assertEquals(config.hosts.size(), 4);
    }
}
