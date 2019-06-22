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

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic utilities
 *
 * @author Steve Hu
 */
public class Util {
    static final Logger logger = LoggerFactory.getLogger(Util.class);

    public static final List<String> METHODS = Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH");

    /**
     * Generate UUID across the entire app and it is used for correlationId.
     *
     * @return String correlationId
     */
    public static String getUUID() {
        UUID id = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return Base64.encodeBase64URLSafeString(bb.array());
    }

    /**
     * Quote the given string if needed
     *
     * @param value The value to quote (e.g. bob)
     * @return The quoted string (e.g. "bob")
     */
    public static String quote(final String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        if (!result.startsWith("\"")) {
            result = "\"" + result;
        }
        if (!result.endsWith("\"")) {
            result = result + "\"";
        }
        return result;
    }

    /**
     * Get InetAddress
     * @return The InetAddress object
     * @deprecated
     */
    public static InetAddress getInetAddress() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (IOException ioe) {
            logger.error("Error in getting InetAddress", ioe);
        }
        return inetAddress;
    }

    public static String getJarVersion() {
        String path = Util.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //String path = "/Users/stevehu/project/light-example-4j/petstore/target/swagger-light-server-1.0.0.jar";
        logger.debug("path = " + path);
        String ver = null;
        if(path.endsWith(".jar")) {
            int endIndex = path.indexOf(".jar");
            int startIndex = path.lastIndexOf("/");
            String jarName = path.substring(startIndex + 1, endIndex);
            ver = jarName.substring(jarName.lastIndexOf("-") + 1);
        }
        return ver;
    }

    public static String getFrameworkVersion() {
        // this doesn't work.
        // TODO make it work.
        Class clazz = Util.class;
        URL location = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
        System.out.println("location = " + location);
        //location = jar:file:/Users/stevehu/project/light-example-4j/petstore/target/swagger-light-server-1.0.0.jar!/com/networknt/utility/Util.class
        return location.toString();
    }

    public static int parseInteger(String intStr) {
        if (intStr == null) {
            return Constants.DEFAULT_INT_VALUE;
        }
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            return Constants.DEFAULT_INT_VALUE;
        }
    }

    public static String urlEncode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLEncoder.encode(value, Constants.DEFAULT_CHARACTER);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String urlDecode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLDecoder.decode(value, Constants.DEFAULT_CHARACTER);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String substituteVariables(String template, Map<String, String> variables) {
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(template);
        // StringBuilder cannot be used here because Matcher expects StringBuffer
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            if (variables.containsKey(matcher.group(1))) {
                String replacement = variables.get(matcher.group(1));
                // quote to work properly with $ and {,} signs
                matcher.appendReplacement(buffer, replacement != null ? Matcher.quoteReplacement(replacement) : "null");
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
