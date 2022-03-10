package com.networknt.config.reload.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.reload.model.ConfigReloadConfig;
import com.networknt.handler.LightHttpHandler;
import com.networknt.server.DefaultConfigLoader;
import com.networknt.server.IConfigLoader;
import com.networknt.status.HttpStatus;
import com.networknt.utility.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigReloadHandler implements LightHttpHandler {

    public static final String CONFIG_NAME = "configreload";
    public static final String STARTUP_CONFIG_NAME = "startup";
    public static final String CONFIG_LOADER_CLASS = "configLoaderClass";
    private static final ObjectMapper mapper = Config.getInstance().getMapper();
    private  static final String STATUS_CONFIG_RELOAD_DISABLED = "ERR12217";
    private  static final String MODULE_DEFAULT = "ALL";
    private  static final String RELOAD_METHOD = "reload";


    public ConfigReloadHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ConfigReloadConfig config = (ConfigReloadConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ConfigReloadConfig.class);
        List<String> modules =  (List) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        if (config.isEnabled()) {
            reLoadConfigs();
            if (modules==null || modules.isEmpty() || MODULE_DEFAULT.equalsIgnoreCase(modules.get(0))) {
                modules.clear();
                Map<String, Object> modulesRegistry =  ModuleRegistry.getRegistry();
                for (Map.Entry<String, Object> entry: modulesRegistry.entrySet()) {
                    modules.add(entry.getKey());
                }
            }

            for (String module: modules) {
                try {
                    Class handler = Class.forName(module);
                    processReloadMethod(handler);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Handler class: " + module + " has not been found");
                }
            }
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(HttpStatus.ACCEPTED.value());
            exchange.getResponseSender().send("config reloaded");
        } else {
            logger.error("Config reload is disabled in configreload.yml");
            setExchangeStatus(exchange, STATUS_CONFIG_RELOAD_DISABLED);
        }
    }

    private void processReloadMethod(Class<?> handler) {
        try {
            Method reload = handler.getDeclaredMethod(RELOAD_METHOD);
            Object processorObject = handler.getDeclaredConstructor().newInstance();
            Object result = reload.invoke(processorObject);
            logger.info("Invoke reload method " + result);
        } catch (Exception e) {
            logger.error("Cannot invoke reload method.");
        }
    }


    private void reLoadConfigs(){
        IConfigLoader configLoader;
        Map<String, Object> startupConfig = Config.getInstance().getJsonMapConfig(STARTUP_CONFIG_NAME);
        if(startupConfig ==null || startupConfig.get(CONFIG_LOADER_CLASS) ==null){
            configLoader = new DefaultConfigLoader();
        }else{
            try {
                Class clazz = Class.forName((String) startupConfig.get(CONFIG_LOADER_CLASS));
                configLoader = (IConfigLoader) clazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("configLoaderClass mentioned in startup.yml could not be found or constructed", e);
            }
        }
        configLoader.reloadConfig();
    }
}
