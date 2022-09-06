package com.networknt.config.reload.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.config.reload.model.ConfigReloadConfig;
import com.networknt.handler.LightHttpHandler;
import com.networknt.status.HttpStatus;
import com.networknt.utility.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is an admin endpoint used to load the list of Registry moduels
 * The endpoint spec will be defined in the openapi-inject-yml
 *
 * @author Gavin Chen
 *
 */
public class ModuleRegistryGetHandler implements LightHttpHandler {

    public static final String CONFIG_NAME = "configreload";
    private static final ObjectMapper mapper = Config.getInstance().getMapper();
    private  static final String STATUS_CONFIG_RELOAD_DISABLED = "ERR12217";

    public ModuleRegistryGetHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {


        ConfigReloadConfig config = (ConfigReloadConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ConfigReloadConfig.class);

        if (config.isEnabled()) {
            List<String> modules = new ArrayList<>();
            Map<String, Object> modulesRegistry =  ModuleRegistry.getRegistry();
            for (Map.Entry<String, Object> entry: modulesRegistry.entrySet()) {
                modules.add(entry.getKey());
            }
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(HttpStatus.OK.value());
            exchange.getResponseSender().send(mapper.writeValueAsString(modules));

        } else {
            logger.error("Config reload is disabled in configreload.yml");
            setExchangeStatus(exchange, STATUS_CONFIG_RELOAD_DISABLED);
        }

    }


}
