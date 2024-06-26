package com.networknt.config.reload.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.reload.model.ConfigReloadConfig;
import com.networknt.consul.ConsulConfig;
import com.networknt.consul.ConsulRegistry;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.server.DefaultConfigLoader;
import com.networknt.server.IConfigLoader;
import com.networknt.status.HttpStatus;
import com.networknt.utility.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is an admin endpoint used to re-load config values from config server on run-time
 * The endpoint spec will be defined in the openapi-inject-yml
 * User call the endpoint will re-load the config values runtime without service restart
 *
 * @author Gavin Chen
 *
 */
public class ConfigReloadHandler implements LightHttpHandler {
    public static final String STARTUP_CONFIG_NAME = "startup";
    public static final String CONFIG_LOADER_CLASS = "configLoaderClass";
    private static final ObjectMapper mapper = Config.getInstance().getMapper();
    private  static final String STATUS_CONFIG_RELOAD_DISABLED = "ERR12217";
    private  static final String MODULE_DEFAULT = "ALL";
    private  static final String RELOAD_METHOD = "reload";

    private static ConfigReloadConfig config;

    public ConfigReloadHandler() {
        if(logger.isDebugEnabled()) logger.debug("ConfigReloadHandler is constructed");
        config = ConfigReloadConfig.load();
        ModuleRegistry.registerModule(ConfigReloadConfig.CONFIG_NAME, ConfigReloadHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ConfigReloadConfig.CONFIG_NAME),null);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (config.isEnabled()) {
            // this modulePlugins list contains both modules and plugins.
            List<String> modulePlugins =  (List)exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
            // the list that contains all the reloaded modules.
            List<String> reloaded =  new ArrayList<>();
            // reload the config values.yml from the config server or local filesystem.
            reLoadConfigs();

            if (modulePlugins==null || modulePlugins.isEmpty() || modulePlugins.contains(MODULE_DEFAULT)) {
                if (modulePlugins == null) modulePlugins = new ArrayList<>();
                if (!modulePlugins.isEmpty()) modulePlugins.clear();
                modulePlugins.addAll(ModuleRegistry.getModuleClasses());
                modulePlugins.addAll(ModuleRegistry.getPluginClasses());
            }

            for (String module: modulePlugins) {
                if (ModuleRegistry.getModuleClasses().contains(module)) {
                    String s = reloadModule(module);
                    if(s != null) reloaded.add(s);
                } else if (ModuleRegistry.getPluginClasses().contains(module)) {
                    String s = reloadPlugin(module);
                    if(s != null) reloaded.add(s);
                } else {
                    logger.error("Module or plugin " + module + " is not found in the registry");
                }
            }
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(HttpStatus.OK.value());
            exchange.getResponseSender().send(mapper.writeValueAsString(reloaded));
        } else {
            logger.error("Config reload is disabled in configReload.yml");
            setExchangeStatus(exchange, STATUS_CONFIG_RELOAD_DISABLED);
        }
    }

    private boolean processReloadMethod(Class<?> handler) {
        try {
            Method reload = handler.getDeclaredMethod(RELOAD_METHOD);
            if(Modifier.isStatic(reload.getModifiers())) {
                Object result = reload.invoke(null, null);
                logger.info("Invoke static reload method " + result);
            } else {
                Object processorObject = handler.getDeclaredConstructor().newInstance();
                Object result = reload.invoke(processorObject);
                logger.info("Invoke reload method " + result);
            }
            return true;
        } catch (Exception e) {
            logger.error("Cannot invoke reload method for :" + handler.getName());
        }
        return false;
    }

    private String reloadModule(String module) {
        try {
            Class handler = Class.forName(module);
            if (processReloadMethod(handler)) {
                logger.info("Reload module " + module);
                return module;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Handler class: " + module + " has not been found");
        }
        return null;
    }

    private String reloadPlugin(String plugin) {
        // remove from the RuleLoaderStartupHook.ruleEngine.actionClassCache
        Object object = RuleLoaderStartupHook.ruleEngine.actionClassCache.remove(plugin);
        if (object != null) {
            // recreate the module and put it into the cache.
            try {
                IAction ia = (IAction)Class.forName(plugin).getDeclaredConstructor().newInstance();
                RuleLoaderStartupHook.ruleEngine.actionClassCache.put(plugin, ia);
            } catch (Exception e) {
                throw new RuntimeException("Handler class: " + plugin + " has not been found");
            }
            logger.info("Reload plugin " + plugin);
            return plugin;
        }
        return null;
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
