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

package com.networknt.mask;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.*;
import com.networknt.config.Config;
import com.networknt.server.ModuleRegistry;
import com.networknt.utility.StringUtils;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to mask sensitive data based on regex pattern before logging
 *
 * @author Steve Hu
 */
public class Mask {

    static Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    private static final String MASK_CONFIG = "mask";
    public static final String MASK_REPLACEMENT_CHAR = "*";
    public static final String MASK_TYPE_STRING = "string";
    public static final String MASK_TYPE_REGEX = "regex";
    public static final String MASK_TYPE_JSON = "json";

    static final Logger logger = LoggerFactory.getLogger(Mask.class);
    private static final Map<String, Object> config;

    static {
        config = Config.getInstance().getJsonMapConfigNoCache(MASK_CONFIG);
        ModuleRegistry.registerModule(MASK_CONFIG, Mask.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MASK_CONFIG), null);
    }

    /**
     * Mask the input string with a list of patterns indexed by key in string section in mask.json
     * This is usually used to mask header values, query parameters and uri parameters
     *
     * @param input String The source of the string that needs to be masked
     * @param key   String The key that maps to a list of patterns for masking in config file
     * @return Masked result
     */
    public static String maskString(String input, String key) {
        if(input == null)
            return null;
        if(config == null)
            return input;
        String output = input;
        Map<String, Object> stringConfig = getConfigMap(MASK_TYPE_STRING);
        if (stringConfig != null) {
            Map<String, Object> keyConfig = (Map<String, Object>) stringConfig.get(key);
            if (keyConfig != null) {
                Set<String> patterns = keyConfig.keySet();
                for (String pattern : patterns) {
                    output = output.replaceAll(pattern, (String) keyConfig.get(pattern));
                }
            }
        }
        return output;
    }

    /**
     * Replace a string input with a pattern found in regex section with key as index. Usually,
     * it is used to replace header, query parameter, uri parameter to same length of stars(*)
     *
     * @param input String The source of the string that needs to be masked
     * @param key   String The key maps to a list of name to pattern pair
     * @param name  String The name of the pattern in the key list
     * @return String Masked result
     */
    public static String maskRegex(String input, String key, String name) {
        if(config == null)
            return input;
        Map<String, Object> regexConfig = getConfigMap(MASK_TYPE_REGEX);
        if (regexConfig != null) {
            Map<String, Object> keyConfig = (Map<String, Object>) regexConfig.get(key);
            if (keyConfig != null) {
                String regex = (String) keyConfig.get(name);
                if (regex != null && regex.length() > 0) {
                    return replaceWithMask(input, MASK_REPLACEMENT_CHAR.charAt(0), regex);
                }
            }
        }
        return input;
    }

    private static String replaceWithMask(String stringToBeMasked, char maskingChar, String regex) {
        if (stringToBeMasked == null || stringToBeMasked.length() == 0)
            return stringToBeMasked;
        String replacementString = "";
        String padGroup;
        if (!StringUtils.isEmpty(regex)) {
            try {
                Pattern pattern = patternCache.get(regex);
                if (pattern == null) {
                    pattern = Pattern.compile(regex);
                    patternCache.put(regex, pattern);
                }
                Matcher matcher = pattern.matcher(stringToBeMasked);
                if (matcher.matches()) {
                    String currentGroup;
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        currentGroup = matcher.group(i + 1);
                        padGroup = StringUtils.rightPad("", currentGroup.length(), maskingChar);
                        stringToBeMasked = StringUtils.replace(stringToBeMasked, currentGroup, padGroup, 1);
                    }
                    replacementString = stringToBeMasked;
                }
            } catch (Exception e) {
                replacementString = StringUtils.rightPad("", stringToBeMasked.length(), maskingChar);
            }
        } else {
            replacementString = StringUtils.rightPad("", stringToBeMasked.length(), maskingChar);
        }
        return replacementString;
    }


    /**
     * Replace values in JSON using json path
     * @param input String The source of the string that needs to be masked
     * @param key String The key maps to a list of json path for masking
     * @return String Masked result
     */
    public static String maskJson(String input, String key) {
        if(input == null)
            return null;
        DocumentContext ctx = JsonPath.parse(input);
        return maskJson(ctx, key);
    }

    /**
     * Replace values in JSON using json path
     * @param input InputStream inputStream of json that needs to be masked
     * @param key String The key maps to a list of json path for masking
     * @return String Masked result
     */
    public static String maskJson(InputStream input, String key) {
        if(input == null)
            return null;
        DocumentContext ctx = JsonPath.parse(input);
        return maskJson(ctx, key);
    }

    /**
     * Replace values in JSON using json path
     * @param input Object POJO of json that needs to be masked
     * @param key String The key maps to a list of json path for masking
     * @return String Masked result
     */
    public static String maskJson(Object input, String key) {
        if(input == null)
            return null;
        DocumentContext ctx = JsonPath.parse(input);
        return maskJson(ctx, key);
    }

    public static String maskJson(DocumentContext ctx, String key) {
        if(ctx == null)
            return null;
        if(config == null)
            return ctx.jsonString();
        Map<String, Object> jsonConfig = getConfigMap(MASK_TYPE_JSON);
        if (jsonConfig != null) {
            Map<String, Object> patternMap = (Map<String, Object>) jsonConfig.get(key);
            if (patternMap != null) {
                JsonNode configNode = Config.getInstance().getMapper().valueToTree(patternMap);
                Iterator<Map.Entry<String, JsonNode>> iterator = configNode.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    applyMask(entry, ctx);
                }
                return ctx.jsonString();
            } else {
                logger.warn("mask.json doesn't contain the key {} ", Encode.forJava(key));
            }
        }
        return ctx.jsonString();
    }

    /**
     * Replace values in JSON using dynamically provided json path rules
     * @param input String The source of the string that needs to be masked
     * @param jsonPathRules Map of json path to mask expression
     * @return String Masked result
     */
    public static String maskJson(String input, Map<String, String> jsonPathRules) {
        if(input == null)
            return null;
        DocumentContext ctx = JsonPath.parse(input);
        return maskJson(ctx, jsonPathRules);
    }

    /**
     * Replace values in JSON using dynamically provided json path rules
     * @param ctx DocumentContext of json that needs to be masked
     * @param jsonPathRules Map of json path to mask expression
     * @return String Masked result
     */
    public static String maskJson(DocumentContext ctx, Map<String, String> jsonPathRules) {
        if(ctx == null || jsonPathRules == null || jsonPathRules.isEmpty())
            return ctx != null ? ctx.jsonString() : null;
        for (Map.Entry<String, String> entry : jsonPathRules.entrySet()) {
            applyMask(entry.getKey(), entry.getValue(), ctx);
        }
        return ctx.jsonString();
    }

    private static void applyMask(Map.Entry<String, JsonNode> entry, DocumentContext ctx) {
        applyMask(entry.getKey(), entry.getValue().asText(), ctx);
    }

    private static void applyMask(String jsonPath, String expression, DocumentContext ctx) {
        Object value;
        try {
            value = ctx.read(jsonPath);
            if (!(value instanceof String || value instanceof Integer || value instanceof List<?>)) {
                logger.error("The value specified by path {} cannot be masked", jsonPath);
            } else {
                if (!(value instanceof List<?>)) {
                    ctx.set(jsonPath, replaceWithMask(value.toString(), MASK_REPLACEMENT_CHAR.charAt(0), expression));
                } else if(value instanceof List<?>){
                    maskList(ctx, jsonPath, expression);
                }
            }
        } catch (PathNotFoundException e) {
            logger.warn("JsonPath {} could not be found.", jsonPath);
        }
    }

    private static void maskList(DocumentContext ctx, String jsonPath, String expression) {
        ctx.configuration().addOptions(Option.AS_PATH_LIST);
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        DocumentContext context = JsonPath.using(conf).parse(ctx.jsonString());
        List<String> pathList = context.read(jsonPath);
        if(pathList != null && pathList.size() == 1) {
            String path = pathList.get(0);
            Object readValues = ctx.read(path);
            List values = new ArrayList();
            if(readValues instanceof String){
                values.add(readValues);
            } else {
                values = ctx.read(path);
            }
            List maskedValue = new ArrayList();
            values.forEach(o -> maskedValue.add(replaceWithMask(o.toString(), MASK_REPLACEMENT_CHAR.charAt(0), expression)));
            ctx.set(path, maskedValue.size() == 1 && !(readValues instanceof List) ? maskedValue.get(0) : maskedValue);
        } else {
            for (String path : Optional.ofNullable(pathList).orElse(Collections.emptyList())) {
                Object value = ctx.read(path);
                ctx.set(path, replaceWithMask(value.toString(), MASK_REPLACEMENT_CHAR.charAt(0), expression));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getConfigMap(String configType) {
        if (config == null) return Collections.emptyMap();
        Object val = config.get(configType);
        if (val == null) return Collections.emptyMap();
        if (val instanceof Map) return (Map<String, Object>) val;
        if (val instanceof String strVal) {
            if (strVal.trim().isEmpty()) return Collections.emptyMap();
            try {
                return Config.getInstance().getMapper().readValue(strVal, Map.class);
            } catch (Exception e) {
                logger.error("Failed to parse " + configType + " from config string: " + strVal, e);
            }
        }
        return Collections.emptyMap();
    }
}
