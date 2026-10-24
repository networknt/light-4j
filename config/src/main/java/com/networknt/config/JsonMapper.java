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

package com.networknt.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.util.List;
import java.util.Map;

public class JsonMapper {
    public static ObjectMapper objectMapper = Jackson2Defaults.builder()
            .enable(tools.jackson.core.StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL)
                    .withContentInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public static String toJson(Object x) {
        try {
            return objectMapper.writeValueAsString(x);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> string2Map(String s) {
        try {
            return objectMapper.readValue(s, new TypeReference<Map<String, Object>>(){});
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, Object>> string2List(String s) {
        try {
            return objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>(){});
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
