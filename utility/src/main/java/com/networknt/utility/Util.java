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
import java.util.*;
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
     * Get well formatted UUID without - and _
     *
     * @return uuid string
     */
    public static String getAlphaNumUUID() {
        String uuid;
        do {
            uuid = getUUID();
        } while (uuid.contains("-") || uuid.contains("_"));
        return uuid;
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

    /**
     * Parses a string of attributes into a Map. This is used to parse the att field from jwt token for fine-grained
     * authorization supported by light-portal.
     *
     * @param attributesString The string of attributes in the format "key1^=^value1~key2^=^value2~..."
     * @return A Map containing the attribute key-value pairs, or an empty Map if the input string is null or empty.
     */
    public static Map<String, String> parseAttributes(String attributesString) {
        Map<String, String> attributeMap = new HashMap<>();

        if (attributesString == null || attributesString.trim().isEmpty()) {
            return attributeMap; // Return empty map for null or empty string
        }

        String[] pairs = attributesString.split("~");
        for (String pair : pairs) {
            String[] keyValue = pair.split("\\^=\\^", 2); // Split into key and value, limit to 2 splits
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                attributeMap.put(key, value);
            }
        }
        return attributeMap;
    }

    /**
     * Get the serviceId from the jsonMap for hybrid framework.
     * @param jsonMap  map that contains host, service, action and version
     * @return serviceId
     */
    public static String getServiceId(Map<String, Object> jsonMap) {
        return  (jsonMap.get("host") == null? "" : jsonMap.get("host") + "/") +
                (jsonMap.get("service") == null? "" : jsonMap.get("service") + "/") +
                (jsonMap.get("action") == null? "" : jsonMap.get("action") + "/") +
                (jsonMap.get("version") == null? "" : jsonMap.get("version"));
    }

    /**
     * Get the serviceId from the host, service, action and version
     * @param host String
     * @param service String
     * @param name String
     * @param version String
     * @return serviceId
     */
    public static String getServiceId(String host, String service, String name, String version) {
        return  (host == null ? "" : host + "/") +
                (service == null ? "" : service + "/") +
                (name == null ? "" : name + "/") +
                (version == null ? "" : version);
    }

}
