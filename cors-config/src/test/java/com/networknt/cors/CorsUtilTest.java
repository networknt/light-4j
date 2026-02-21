/*
 * Copyright (C) 2015 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.networknt.cors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testing CORS utility class.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2015 Red Hat, inc.
 */
public class CorsUtilTest {

    public CorsUtilTest() {
    }

    /**
     * Test of sanitizeDefaultPort method, of class CorsUtil.
     */
    @Test
    public void testSanitizeDefaultPort() {
        String url = "http://127.0.0.1:80";
        Assertions.assertEquals("http://127.0.0.1", CorsUtil.sanitizeDefaultPort(url));
        url = "http://127.0.0.1";
        Assertions.assertEquals("http://127.0.0.1", CorsUtil.sanitizeDefaultPort(url));
        url = "http://127.0.0.1:443";
        Assertions.assertEquals("http://127.0.0.1:443", CorsUtil.sanitizeDefaultPort(url));
        url = "http://127.0.0.1:7080";
        Assertions.assertEquals("http://127.0.0.1:7080", CorsUtil.sanitizeDefaultPort(url));
        url = "https://127.0.0.1:80";
        Assertions.assertEquals("https://127.0.0.1:80", CorsUtil.sanitizeDefaultPort(url));
        url = "https://127.0.0.1:443";
        Assertions.assertEquals("https://127.0.0.1", CorsUtil.sanitizeDefaultPort(url));
        url = "https://127.0.0.1";
        Assertions.assertEquals("https://127.0.0.1", CorsUtil.sanitizeDefaultPort(url));
        url = "http://[::FFFF:129.144.52.38]:7080";
        Assertions.assertEquals("http://[::FFFF:129.144.52.38]:7080", CorsUtil.sanitizeDefaultPort(url));
        url = "http://[::FFFF:129.144.52.38]:80";
        Assertions.assertEquals("http://[::FFFF:129.144.52.38]", CorsUtil.sanitizeDefaultPort(url));
    }

    /**
     * Test of defaultOrigin method, of class CorsUtil.
     */
    @Test
    public void testDefaultOrigin() {
        Assertions.assertEquals("http://localhost", CorsUtil.defaultOrigin("http", "localhost", 80));
        Assertions.assertEquals("http://www.example.com:7080", CorsUtil.defaultOrigin("http", "www.example.com", 7080));
        Assertions.assertEquals("https://www.example.com", CorsUtil.defaultOrigin("https", "www.example.com", 443));
        Assertions.assertEquals("http://[::1]", CorsUtil.defaultOrigin("http", "[::1]", 80));
    }
}
