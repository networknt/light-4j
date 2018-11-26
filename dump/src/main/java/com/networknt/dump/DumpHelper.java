package com.networknt.dump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DumpHelper {

    static Logger logger = LoggerFactory.getLogger(DumpHelper.class);
    private static int TAB_SIZE = 4;

    static void dumpBasedOnOption(Object configObject, IDumpable dumpable) {
        if(configObject instanceof Boolean) {
            dumpable.dumpOption((Boolean)configObject);
        } else if(configObject instanceof Map<?, ?>) {
            dumpable.dumpOption((Map)configObject);
        } else if(configObject instanceof List<?>) {
            dumpable.dumpOption((List<?>) configObject);
        } else {
            logger.error("configuration is incorrect for {}", configObject.toString());
        }
    }

    static String[] getSupportHttpMessageOptions(IDumpable.HttpMessageType type) {
        return IDumpable.HttpMessageType.RESPONSE.equals(type) ? DumpHandler.RESPONSE_OPTIONS : DumpHandler.REQUEST_OPTIONS;
    }

    static void logResult(Map<String, Object> result, int level) {
        level += 1;
        for(Map.Entry<String, Object> entry: result.entrySet()) {
            if(entry.getValue() instanceof Map<?, ?>) {
                logger.info(getTabBasedOnLevel(level) + "{}:",entry.getKey());
                logResult((Map)entry.getValue(), level);
            } else if(entry.getValue() instanceof String) {
                logger.info(getTabBasedOnLevel(level) + "{}: {}",entry.getKey(), (String)entry.getValue());
            } else {
                logger.debug("Cannot handle this type: {}", entry.getKey());
            }
        }
    }

    static String getTabBasedOnLevel(int level) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < level; i ++) {
            for(int j = 0; j < TAB_SIZE; j++) {
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
