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
package com.networknt.jaeger.tracing;

import com.networknt.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JaegerConfigTest {

    @Test
    public void testTrue() {
        JaegerConfig jaegerConfig = (JaegerConfig) Config.getInstance().getJsonObjectConfig("jaeger-tracing-true", JaegerConfig.class);
        Assertions.assertTrue(jaegerConfig.isEnabled());
    }

    @Test
    public void testFalse() {
        JaegerConfig jaegerConfig = (JaegerConfig) Config.getInstance().getJsonObjectConfig("jaeger-tracing-false", JaegerConfig.class);
        Assertions.assertFalse(jaegerConfig.isEnabled());
    }

    @Test
    public void testParamInteger() {
        JaegerConfig jaegerConfig = (JaegerConfig) Config.getInstance().getJsonObjectConfig("jaeger-tracing-true", JaegerConfig.class);
        Assertions.assertEquals(jaegerConfig.getParam(), 1000);
    }

    @Test
    public void testParamDouble() {
        JaegerConfig jaegerConfig = (JaegerConfig) Config.getInstance().getJsonObjectConfig("jaeger-tracing-false", JaegerConfig.class);
        Assertions.assertEquals(jaegerConfig.getParam(), 0.5);
    }
}
