package com.networknt.config.reload.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.config.ConfigLoader;
import com.networknt.config.reload.model.ConfigReloadConfig;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.server.DefaultConfigLoader;
import com.networknt.server.IConfigLoader;
import com.networknt.status.HttpStatus;
import com.networknt.server.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

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

    public ConfigReloadHandler() {
        ConfigReloadConfig.load();
        if(logger.isDebugEnabled()) logger.debug("ConfigReloadHandler is constructed");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ConfigReloadConfig config = ConfigReloadConfig.load();
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
                    logger.error("Module or plugin {} is not found in the registry", module);
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

    private String reloadModule(String module) {
        String configName = findConfigName(module);
        if (configName != null) {
            Config.getInstance().clearConfigCache(configName);
            logger.info("Reload module {} with config {}", module, configName);
            return module;
        } else {
            logger.error("Module {} cannot find config name in the module registry", module);
        }
        return null;
    }

    private String reloadPlugin(String plugin) {
        String configName = findConfigName(plugin);
        if (configName != null) {
            Config.getInstance().clearConfigCache(configName);
            logger.info("Reload plugin {} with config {}", plugin, configName);
        }
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
            logger.info("Reload plugin {}", plugin);
            return plugin;
        }
        return null;
    }

    private String findConfigName(String moduleClass) {
        // Check module registry
        for (String key : ModuleRegistry.getModuleRegistry().keySet()) {
            if (key.endsWith(":" + moduleClass)) {
                return key.substring(0, key.lastIndexOf(":"));
            }
        }
        // Check plugin registry
        for (String key : ModuleRegistry.getPluginRegistry().keySet()) {
            if (key.endsWith(":" + moduleClass)) {
                return key.substring(0, key.lastIndexOf(":"));
            }
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
