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

    @Test
    public void testIsJwtToken() {
        String jwtBearer = "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTk0MTEyMjc3MiwianRpIjoiTkc4NWdVOFR0SEZuSThkS2JsQnBTUSIsImlhdCI6MTYyNTc2Mjc3MiwibmJmIjoxNjI1NzYyNjUyLCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjdkNDIzNDgtYzY0Ny00ZWZiLWE1MmQtNGM1Nzg3NDIxZTcyIiwic2NvcGUiOiJwb3J0YWwuciBwb3J0YWwudyIsInNlcnZpY2UiOiIwMTAwIn0.Q6BN5CGZL2fBWJk4PIlfSNXpnVyFhK6H8X4caKqxE1XAbX5UieCdXazCuwZ15wxyQJgWCsv4efoiwO12apGVEPxIc7gpvctPrRIDo59dmTjfWH0p3ja0Zp8tYLD-5Sh65WUtJtkvPQk0uG96JJ64Da28lU4lGFZaCvkaS-Et9Wn0BxrlCE5_ta66Qc9t4iUMeAsAHIZJffOBsREFhOpC0dKSXBAyt9yuLDuDt9j7HURXBHyxSBrv8Nj_JIXvKhAxquffwjZF7IBqb3QRr-sJV0auy-aBQ1v8dYuEyIawmIP5108LH8QdH-K8NkI1wMnNOz_wWDgixOcQqERmoQ_Q3g";
        String jwt = "eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTk0MTEyMjc3MiwianRpIjoiTkc4NWdVOFR0SEZuSThkS2JsQnBTUSIsImlhdCI6MTYyNTc2Mjc3MiwibmJmIjoxNjI1NzYyNjUyLCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjdkNDIzNDgtYzY0Ny00ZWZiLWE1MmQtNGM1Nzg3NDIxZTcyIiwic2NvcGUiOiJwb3J0YWwuciBwb3J0YWwudyIsInNlcnZpY2UiOiIwMTAwIn0.Q6BN5CGZL2fBWJk4PIlfSNXpnVyFhK6H8X4caKqxE1XAbX5UieCdXazCuwZ15wxyQJgWCsv4efoiwO12apGVEPxIc7gpvctPrRIDo59dmTjfWH0p3ja0Zp8tYLD-5Sh65WUtJtkvPQk0uG96JJ64Da28lU4lGFZaCvkaS-Et9Wn0BxrlCE5_ta66Qc9t4iUMeAsAHIZJffOBsREFhOpC0dKSXBAyt9yuLDuDt9j7HURXBHyxSBrv8Nj_JIXvKhAxquffwjZF7IBqb3QRr-sJV0auy-aBQ1v8dYuEyIawmIP5108LH8QdH-K8NkI1wMnNOz_wWDgixOcQqERmoQ_Q3g";
        String swt = "4VHHkOUfQQuInU5auCHFvw";

        Assert.assertTrue(StringUtils.isJwtToken(jwtBearer));
        Assert.assertTrue(StringUtils.isJwtToken(jwt));
        Assert.assertFalse(StringUtils.isJwtToken(swt));
    }

    @Test
    public void testMaskHalfString() {
        String s = "1234567890";
        Assert.assertEquals("*****67890", StringUtils.maskHalfString(s));
        s = "123456789";
        Assert.assertEquals("****56789", StringUtils.maskHalfString(s));
    }

    @Test
    public void testMatchPath() {
        String pattern = "/v1/pets/{petId}";
        String path = "/v1/pets/1";
        Assert.assertTrue(StringUtils.matchPathToPattern(path, pattern));
        pattern = "/v1/pets/{petId}/name";
        path = "/v1/pets/1/name";
        Assert.assertTrue(StringUtils.matchPathToPattern(path, pattern));
        pattern = "/v1/pets/{petId}";
        Assert.assertTrue(StringUtils.matchPathToPattern(path, pattern));

        pattern = "/foo/bar";
        Assert.assertTrue(StringUtils.matchPathToPattern("/foo/bar", pattern));
        Assert.assertFalse(StringUtils.matchPathToPattern("/foo/bar?abc=123", pattern));
    }
}
