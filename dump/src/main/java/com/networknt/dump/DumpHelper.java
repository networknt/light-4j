package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DumpHelper {

    static Logger logger = LoggerFactory.getLogger(DumpHelper.class);

    static void dumpBasedOnOption(Map<String, Object>result, HttpServerExchange exchange, Object configObject, IDumpable dumpable) {
        if(configObject instanceof Boolean) {
            dumpable.dumpOption(result, exchange, (Boolean)configObject);
        } else if(configObject instanceof Map<?, ?>) {
            dumpable.dumpOption(result, exchange, (Map)configObject);
        } else if(configObject instanceof List<?>) {
            dumpable.dumpOption(result, exchange, (List<?>) configObject);
        } else {
            logger.error("configuration is incorrect for {}", configObject.toString());
        }
    }
}
