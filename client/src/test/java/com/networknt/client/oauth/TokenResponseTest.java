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

package com.networknt.client.oauth;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.networknt.config.Config;
import org.junit.Test;

import java.io.IOException;

public class TokenResponseTest {
    @Test
    public void testExactFields() throws IOException {
        String s = "{\"access_token\":\"access_token\",\"token_type\":\"token_type\",\"expires_in\":3600,\"scope\":\"scope\",\"state\":\"state\",\"refresh_token\":\"refresh_token\",\"example_parameter\":\"example_parameter\"}";
        TokenResponse tokenResponse = Config.getInstance().getMapper().readValue(s, TokenResponse.class);
    }

    @Test
    public void testMissingFields() throws IOException {
        String s = "{\"access_token\":\"access_token\",\"token_type\":\"token_type\",\"expires_in\":3600}";
        TokenResponse tokenResponse = Config.getInstance().getMapper().readValue(s, TokenResponse.class);
    }

    @Test
    public void testErrorStatus() throws IOException {
        String s = "{\"statusCode\":401,\"code\":\"ERR10001\",\"message\":\"AUTH_TOKEN_EXPIRED\",\"description\":\"Jwt token in authorization header expired\"}";
        TokenResponse tokenResponse = Config.getInstance().getMapper().readValue(s, TokenResponse.class);
    }


    @Test(expected = UnrecognizedPropertyException.class)
    public void testExtraFields() throws IOException {
        String s = "{\"access_token\":\"access_token\",\"token_type\":\"token_type\",\"expires_in\":3600,\"scope\":\"scope\",\"state\":\"state\",\"refresh_token\":\"refresh_token\",\"example_parameter\":\"example_parameter\",\"extra\":\"extra\"}";
        TokenResponse tokenResponse = Config.getInstance().getMapper().readValue(s, TokenResponse.class);
    }
}
