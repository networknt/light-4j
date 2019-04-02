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

package com.networknt.dump;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * a helper class for com.networknt.dump, contains impl of logging a Map<String, Object>
 */
class DumpHelper {
    private static Logger logger = LoggerFactory.getLogger(DumpHandler.class);

    /**
     * A help method to log result pojo
     * @param result the map contains info that needs to be logged
     */
    static void logResult(Map<String, Object> result, DumpConfig config) {
        Consumer<String> loggerFunc = getLoggerFuncBasedOnLevel(config.getLogLevel());
        if(config.isUseJson()) {
            logResultUsingJson(result, loggerFunc);
        } else {
            int startLevel = -1;
            StringBuilder sb = new StringBuilder("Http request/response information:");
            _logResult(result, startLevel, config.getIndentSize(), sb);
            loggerFunc.accept(sb.toString());
        }
    }

    /**
     *  this method actually append result to result string
     */
    private static <T> void _logResult(T result, int level, int indentSize, StringBuilder info) {
        if(result instanceof Map) {
            level += 1;
            int finalLevel = level;
            ((Map)result).forEach((k, v) -> {
                info.append("\n");
                info.append(getTabBasedOnLevel(finalLevel, indentSize))
                        .append(k.toString())
                        .append(":");
                _logResult(v, finalLevel, indentSize, info);
            });
        } else if(result instanceof List) {
            int finalLevel = level;
            ((List)result).forEach(element -> _logResult(element, finalLevel, indentSize, info));
        } else if(result instanceof String) {
            info.append(" ").append(result);
        } else if(result != null) {
            try {
                logger.warn(getTabBasedOnLevel(level, indentSize) + "{}", result);
            } catch (Exception e) {
                logger.error("Cannot handle this type: {}", result.getClass().getTypeName());
            }
        }
    }

    /**
     *
     * @param result a Map<String, Object> contains http request/response info which needs to be logged.
     * @param loggerFunc Consuer<T> getLoggerFuncBasedOnLevel(config.getLogLevel())
     */
    private static void logResultUsingJson(Map<String, Object> result, Consumer<String> loggerFunc) {
        ObjectMapper mapper = new ObjectMapper();
        String resultJson = "";
        try {
            resultJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
        if(StringUtils.isNotBlank(resultJson)){
            loggerFunc.accept("Dump Info:\n" + resultJson);
        }
    }

    /**
     * calculate indent for formatting
     * @return "   " string of empty spaces
     */
    private static String getTabBasedOnLevel(int level, int indentSize) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < level; i ++) {
            for(int j = 0; j < indentSize; j++) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * @param level type: String, the level the logger will log to
     * @return Consumer<String>
     */
    private static Consumer<String> getLoggerFuncBasedOnLevel(String level) {
        switch(level.toUpperCase()) {
            case "ERROR":
                return logger::error;
            case "INFO":
                return logger::info;
            case "DEBUG":
                return logger::debug;
            case "WARN":
                return logger::warn;
            default:
                return logger::info;
        }
    }
}
