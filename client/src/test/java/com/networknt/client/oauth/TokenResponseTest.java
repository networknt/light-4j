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
