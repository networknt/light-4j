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

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TokenResponseTest {
    static Stream<Arguments> successfulResponses() {
        String complete = "{\"access_token\":\"access_token\",\"token_type\":\"token_type\",\"expires_in\":3600,\"scope\":\"scope\",\"state\":\"state\",\"refresh_token\":\"refresh_token\",\"example_parameter\":\"example_parameter\"}";
        String minimal = "{\"access_token\":\"access_token\",\"token_type\":\"token_type\",\"expires_in\":3600}";
        String extra = complete.substring(0, complete.length() - 1) + ",\"extra\":\"extra\"}";
        return Stream.of(
                Arguments.of("all fields", complete, true),
                Arguments.of("missing optional fields", minimal, false),
                Arguments.of("unknown fields ignored", extra, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("successfulResponses")
    void bindsSuccessfulResponse(String scenario, String json, boolean hasOptionalFields) {
        TokenResponse response = Config.getInstance().getMapper().readValue(json, TokenResponse.class);
        assertAll(scenario,
                () -> assertEquals("access_token", response.getAccessToken()),
                () -> assertEquals("token_type", response.getTokenType()),
                () -> assertEquals(3600L, response.getExpiresIn()),
                () -> assertEquals(hasOptionalFields ? "scope" : null, response.getScope()),
                () -> assertEquals(hasOptionalFields ? "state" : null, response.getState()),
                () -> assertEquals(hasOptionalFields ? "refresh_token" : null, response.getRefreshToken()),
                () -> assertEquals(hasOptionalFields ? "example_parameter" : null, response.getExampleParameter()),
                () -> assertEquals(0, response.getStatusCode()));
    }

    @Test
    public void testErrorStatus() {
        String s = "{\"statusCode\":401,\"code\":\"ERR10001\",\"message\":\"AUTH_TOKEN_EXPIRED\",\"description\":\"Jwt token in authorization header expired\"}";
        TokenResponse response = Config.getInstance().getMapper().readValue(s, TokenResponse.class);
        assertAll(
                () -> assertEquals(401, response.getStatusCode()),
                () -> assertEquals("ERR10001", response.getCode()),
                () -> assertEquals("AUTH_TOKEN_EXPIRED", response.getMessage()),
                () -> assertEquals("Jwt token in authorization header expired", response.getDescription()),
                () -> assertNull(response.getAccessToken()),
                () -> assertEquals(0L, response.getExpiresIn()));
    }
}
