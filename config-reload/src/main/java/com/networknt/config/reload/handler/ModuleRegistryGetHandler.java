package com.networknt.config.reload.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.config.reload.model.ConfigReloadConfig;
import com.networknt.handler.LightHttpHandler;
import com.networknt.status.HttpStatus;
import com.networknt.server.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an admin endpoint used to load the list of Registry moduels
 * The endpoint spec will be defined in the openapi-inject-yml
 *
 * @author Gavin Chen
 *
 */
public class ModuleRegistryGetHandler implements LightHttpHandler {

    private static final ObjectMapper mapper = Config.getInstance().getMapper();
    private static final String STATUS_CONFIG_RELOAD_DISABLED = "ERR12217";

    public ModuleRegistryGetHandler() {
        ConfigReloadConfig.load();
        if(logger.isDebugEnabled()) logger.debug("ModuleRegistryGetHandler is constructed");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ConfigReloadConfig config = ConfigReloadConfig.load();
        if (config.isEnabled()) {
            List<String> modulePlugins = new ArrayList<>();
            modulePlugins.addAll(ModuleRegistry.getModuleClasses());
            modulePlugins.addAll(ModuleRegistry.getPluginClasses());

            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(HttpStatus.OK.value());
            exchange.getResponseSender().send(mapper.writeValueAsString(modulePlugins));
        } else {
            logger.error("Config reload is disabled in configReload.yml");
            setExchangeStatus(exchange, STATUS_CONFIG_RELOAD_DISABLED);
        }
    }
}
