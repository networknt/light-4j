package com.networknt.dump;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class DumpHelper {

    private static final String INDENT_SIZE = "indentSize";
    private static final int DEFAULT_INDENT_SIZE = 4;

    private static Logger logger = LoggerFactory.getLogger(DumpHandler.class);

    /**
     * A help method to log result pojo
     * @param result the map contains info that needs to be logged
     * @param indentSize indentSize for each line
     * @param useJson if use default json format
     * @param logFunc use Logger.error(), Logger.info() or Logger.debug() to log based on different level
     */
    static void logResult(Map<String, Object> result, int indentSize, boolean useJson, Consumer<String> logFunc) {
        if(useJson) {
            logResultUsingJson(result);
        } else {
            int startLevel = -1;
            StringBuilder sb = new StringBuilder("Http request/response information:");
            _logResult(result, startLevel, indentSize, sb);
            logFunc.accept(sb.toString());
        }
    }

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
                logger.info(getTabBasedOnLevel(level, indentSize) + "{}", result);
            } catch (Exception e) {
                logger.error("Cannot handle this type: {}", result.getClass().getTypeName());
            }
        }
    }

    private static void logResultUsingJson(Map<String, Object> result) {
        ObjectMapper mapper = new ObjectMapper();
        String resultJson = "";
        try {
            resultJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
        if(StringUtils.isNotBlank(resultJson)){
            logger.info("Dump Info:\n" + resultJson);
        }
    }

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
     *
     * @param config the config map
     * @param optionName option name of config map above
     * @return true if option has value and the value is not false
     */
    static boolean checkIfOptionTruthy(Map<String, Object> config, String optionName) {
        Object option = config.get(optionName);
        if(option instanceof Map || option instanceof List) {
            return true;
        } else if(option instanceof Boolean) {
            return (Boolean)option;
        } else if(option == null){
            return false;
        } else {
            logger.error("cannot handle option type for option: {}", optionName);
            return false;
        }
    }

    static int getIndentSize(Map<String, Object> config) {
        Object indentSize = config.get(INDENT_SIZE);
        if(indentSize instanceof Integer) {
            return (int)config.get(INDENT_SIZE);
        } else {
            return DEFAULT_INDENT_SIZE;
        }
    }

    static boolean checkIfUseJson(Map<String, Object> config) {
        Object useJson = config.get(DumpConstants.USE_JSON);
        return useJson instanceof Boolean && (Boolean) useJson;
    }

    static Consumer<String> getLoggerFuncBasedOnLevel(String level) {
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


    static Consumer<String> getLoggerFunc(Map<String, Object> config) {
        Object logLevel = config.get(DumpConstants.LOG_LEVEL);
        return logLevel instanceof String ? DumpHelper.getLoggerFuncBasedOnLevel((String)logLevel) : DumpHelper.getLoggerFuncBasedOnLevel("");
    }
}
