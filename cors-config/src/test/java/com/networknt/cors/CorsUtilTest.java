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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("http://127.0.0.1"));
        url = "http://127.0.0.1";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("http://127.0.0.1"));
        url = "http://127.0.0.1:443";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("http://127.0.0.1:443"));
        url = "http://127.0.0.1:7080";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("http://127.0.0.1:7080"));
        url = "https://127.0.0.1:80";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("https://127.0.0.1:80"));
        url = "https://127.0.0.1:443";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("https://127.0.0.1"));
        url = "https://127.0.0.1";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("https://127.0.0.1"));
        url = "http://[::FFFF:129.144.52.38]:7080";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("http://[::FFFF:129.144.52.38]:7080"));
        url = "http://[::FFFF:129.144.52.38]:80";
        assertThat(CorsUtil.sanitizeDefaultPort(url), is("http://[::FFFF:129.144.52.38]"));
    }

    /**
     * Test of defaultOrigin method, of class CorsUtil.
     */
    @Test
    public void testDefaultOrigin() {
        assertThat(CorsUtil.defaultOrigin("http", "localhost", 80), is("http://localhost"));
        assertThat(CorsUtil.defaultOrigin("http", "www.example.com", 7080), is("http://www.example.com:7080"));
        assertThat(CorsUtil.defaultOrigin("https", "www.example.com", 443), is("https://www.example.com"));
        assertThat(CorsUtil.defaultOrigin("http", "[::1]", 80), is("http://[::1]"));
    }
}
