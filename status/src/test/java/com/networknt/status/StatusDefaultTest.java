/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by steve on 23/09/16.
 */
@RunWith(SeparateClassloaderTestRunner.class)
public class StatusDefaultTest {

    @Test
    public void testConstructor() {
        Status status = new Status("ERR10001");
        Assert.assertEquals(401, status.getStatusCode());
    }

    @Test
    public void testConstructorMissingArgs() {
        Status status = new Status("ERR10048");
        Assert.assertEquals(404, status.getStatusCode());
        Assert.assertTrue(status.getDescription().contains("%s"));
    }

    @Test
    public void testToString() {
        Status status = new Status("ERR10001");
        System.out.println(status);
        Assert.assertEquals("{\"statusCode\":401,\"code\":\"ERR10001\",\"message\":\"AUTH_TOKEN_EXPIRED\",\"description\":\"Jwt token in authorization header expired\",\"severity\":\"ERROR\"}", status.toString());
    }

    @Test
    public void testToStringWithArgs() {
        Status status = new Status("ERR11000", "parameter name", "original url");
        System.out.println(status);
        Assert.assertEquals("{\"statusCode\":400,\"code\":\"ERR11000\",\"message\":\"VALIDATOR_REQUEST_PARAMETER_QUERY_MISSING\",\"description\":\"Query parameter parameter name is required on path original url but not found in request.\",\"severity\":\"ERROR\"}", status.toString());
    }

    @Test
    public void testToStringWithoutSeverity() {
        Status status = new Status(400, "ERR11000", "INVALID_AUTH_TOKEN","Incorrect signature or malformed token in authorization header");
        System.out.println(status);
        Assert.assertEquals("{\"statusCode\":400,\"code\":\"ERR11000\",\"message\":\"INVALID_AUTH_TOKEN\",\"description\":\"Incorrect signature or malformed token in authorization header\",\"severity\":\"ERROR\"}", status.toString());
    }

    @Test
    public void testToStringWithAllArgs() {
        Status status = new Status(400, "ERR11000", "INVALID_AUTH_TOKEN","Incorrect signature or malformed token in authorization header", "SEVERE");
        System.out.println(status);
        Assert.assertEquals("{\"statusCode\":400,\"code\":\"ERR11000\",\"message\":\"INVALID_AUTH_TOKEN\",\"description\":\"Incorrect signature or malformed token in authorization header\",\"severity\":\"SEVERE\"}", status.toString());
    }

    @Test
    public void testToStringPerf() {
        long start = System.currentTimeMillis();
        Status status = new Status("ERR10001");
        String s = null;
        for(int i = 0; i < 1000000; i++) {
            s = status.toString();
        }
        System.out.println("ToString Perf " + (System.currentTimeMillis() - start));
    }

    /*
    @Test
    public void testToStringAppendPerf() {
        long start = System.currentTimeMillis();
        Status status = new Status("ERR10001");
        String s = null;
        for(int i = 0; i < 1000000; i++) {
            s = status.toStringAppend();
        }
        System.out.println("ToStringAppend Perf " + (System.currentTimeMillis() - start));
    }
    */

    @Test
    public void testJacksonPerf() throws JsonProcessingException {
        ObjectMapper mapper = Config.getInstance().getMapper();
        long start = System.currentTimeMillis();
        Status status = new Status("ERR10001");
        String s = null;
        for(int i = 0; i < 1000000; i++) {
            s = mapper.writeValueAsString(status);
        }
        System.out.println("Jackson Perf " + (System.currentTimeMillis() - start));
    }
}
