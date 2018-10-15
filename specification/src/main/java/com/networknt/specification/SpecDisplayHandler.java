
package com.networknt.specification;

import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/**
 *  Display API Specification
 *
 * @author Gavin Chen
 */
public class SpecDisplayHandler implements LightHttpHandler {
    public static final String CONFIG_NAME = "specification";

    public SpecDisplayHandler(){
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        SpecificationConfig config = (SpecificationConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, SpecificationConfig.class);
        final String payload = Config.getInstance().getStringFromFile(config.getFileName());
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), config.getContentType());
        exchange.getResponseSender().send(payload);
    }

}
