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

package com.networknt.consul;

import com.networknt.config.Config;
import com.networknt.consul.client.ConsulClientImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConsulClientImplTest {

    private static final ConsulConfig config = (ConsulConfig) Config.getInstance().getJsonObjectConfig(ConsulConstants.CONFIG_NAME, ConsulConfig.class);

   @Test
    public void testWaitProperty() {
        assertEquals("600s", config.getWait());

    }
}
