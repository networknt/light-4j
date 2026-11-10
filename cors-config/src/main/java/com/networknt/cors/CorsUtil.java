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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for CORS handling.
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class CorsUtil {
    private static final Logger logger = LoggerFactory.getLogger(CorsUtil.class);

    /**
     * Determine the default origin, to allow for local access.
     * @param protocol, the protocol.
     * @param host, the host.
     * @param port, the port.
     * @return the default origin (aka current server).
     */
    public static String defaultOrigin(String protocol, String host, int port) {
        //This browser set header should not need IPv6 escaping
        StringBuilder allowedOrigin = new StringBuilder(256);
        allowedOrigin.append(protocol).append("://").append(host);
        if (!isDefaultPort(port, protocol)) {
            allowedOrigin.append(':').append(port);
        }
        return allowedOrigin.toString();
    }

    private static boolean isDefaultPort(int port, String protocol) {
        return (("http".equals(protocol) && 80 == port) || ("https".equals(protocol) && 443 == port));
    }

    /**
     * Removes the port from a URL if this port is the default one for the URL's scheme.
     * @param url the url to be sanitized.
     * @return the sanitized origin, or unchanged input if malformed.
     * @deprecated retained for source compatibility; origin policies normalize ports during matching.
     */
    @Deprecated
    public static String sanitizeDefaultPort(String url) {
        return CorsOriginPolicy.sanitizeDefaultPort(url);
    }

}
