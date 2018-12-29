package com.networknt.dump;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

class DumpHelper {

    private static Logger logger = LoggerFactory.getLogger(DumpHelper.class);

    static void logResult(Map<String, Object> result, int indentSize, boolean useJson) {
        if(useJson) {
            logResultUsingJson(result);
        } else {
            int startLevel = -1;
            StringBuilder sb = new StringBuilder("Http request/response information:");
            _logResult(result, startLevel, indentSize, sb);
            logger.info(sb.toString());
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

    //return true when an option is not written as 'true'
    static Boolean checkOptionNotFalse(Object option) {
        return (option instanceof Boolean && (Boolean) option)
                || (!(option instanceof Boolean) && option != null);
    }
}
