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

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * Created by steve on 12/07/17.
 */
public class NioUtilsTest {

    @Test
    public void testTempDir() {
        String tempDir = NioUtils.getTempDir();
        System.out.println("tempDir = " + tempDir);
    }

    @Test
    public void testList() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("rest.zip");
        NioUtils.list(url.getPath().toString().replace("/C:/","C:\\"));
    }

    @Test
    public void testUnzip() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("rest.zip");
        NioUtils.unzip(url.getPath().toString().replace("/C:/","C:\\"), NioUtils.getTempDir());
    }

}
