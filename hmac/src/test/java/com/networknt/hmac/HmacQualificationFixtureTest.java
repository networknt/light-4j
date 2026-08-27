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

package com.networknt.hmac;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Properties;

/** Validates the language-neutral raw request fixture shared by qualification suites. */
class HmacQualificationFixtureTest {
    @Test
    void publishedGithubFixtureIsByteExactAndSelfConsistent() throws Exception {
        Properties fixture = loadFixture();
        byte[] secret = Base64.getDecoder().decode(fixture.getProperty("secretBase64"));
        byte[] body = Base64.getDecoder().decode(fixture.getProperty("bodyBase64"));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));

        Assertions.assertEquals("1", fixture.getProperty("fixtureVersion"));
        Assertions.assertEquals("github", fixture.getProperty("profile"));
        Assertions.assertEquals("POST", fixture.getProperty("method"));
        Assertions.assertEquals("sha256=", fixture.getProperty("signaturePrefix"));
        Assertions.assertEquals(fixture.getProperty("signatureHex"),
                HexFormat.of().formatHex(mac.doFinal(body)));
    }

    private Properties loadFixture() throws IOException {
        Properties fixture = new Properties();
        try (InputStream input = getClass().getResourceAsStream(
                "/qualification/hmac-webhook-v1.properties")) {
            Assertions.assertNotNull(input, "qualification fixture must be packaged as a test resource");
            fixture.load(input);
        }
        return fixture;
    }
}
