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

package com.networknt.utility;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StringUtilsTest {
    @Test
    @Ignore
    public void testExpandEnvVars() {
        String s = "IP=${DOCKER_HOST_IP}";
        Assert.assertEquals("IP=192.168.1.120", StringUtils.expandEnvVars(s));
    }

    @Test
    public void testInputStreamToString_withExpected() throws IOException {
        String expected = "test data";
        InputStream anyInputStream = new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8));
        String actual = StringUtils.inputStreamToString(anyInputStream, StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }
}
