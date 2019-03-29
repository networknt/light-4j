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

package com.networknt.decode;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class RequestDecodeConfigTest {
    @Test
    public void loadConfig() {
        RequestDecodeConfig config =
                (RequestDecodeConfig) Config.getInstance().getJsonObjectConfig(RequestDecodeConfig.CONFIG_NAME, RequestDecodeConfig.class);
        Assert.assertEquals(config.getDecoders().size(), 2);
    }
}
