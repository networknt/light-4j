package com.networknt.dump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class DumpHelper {

    private static Logger logger = LoggerFactory.getLogger(DumpHelper.class);

    static void logResult(Map<String, Object> result, int indentSize) {
        int startLevel = -1;
        _logResult(result, startLevel, indentSize);
    }
    private static void _logResult(Map<String, Object> result, int level, int indentSize) {
        level += 1;
        for(Map.Entry<String, Object> entry: result.entrySet()) {
            if(entry.getValue() instanceof Map<?, ?>) {
                logger.info(getTabBasedOnLevel(level, indentSize) + "{}:",entry.getKey());
                _logResult((Map)entry.getValue(), level, indentSize);
            } else if(entry.getValue() instanceof String) {
                logger.info(getTabBasedOnLevel(level, indentSize) + "{}: {}",entry.getKey(), entry.getValue(), indentSize);
            } else if(entry.getValue() != null){
                try{
                    logger.info(getTabBasedOnLevel(level, indentSize) + "{}: {}", entry.getKey() , entry.getValue().toString());
                } catch (Exception e) {
                    logger.error("Cannot handle this type: {}", entry.getKey());
                }
            }
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
    public static Boolean checkOptionNotFalse(Object option) {
        return (option instanceof Boolean && (Boolean) option)
                || (!(option instanceof Boolean) && option != null);
    }
}
