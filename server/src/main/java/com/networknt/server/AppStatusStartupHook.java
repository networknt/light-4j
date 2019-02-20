package com.networknt.server;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The startup hook is used to merge status.yml and app-status.yml
 * <p>
 * 1. If duplicated elements is contained by these two files, throw a RuntimeException when startup.
 * 2. If elements contained by app-status.yml is not contained by status.yml, append them to status.yml
 * <p>
 * To implement this startup hook, uncomment it in service.yml
 *
 * @author Jiachen Sun
 */
public class AppStatusStartupHook implements StartupHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(AppStatusStartupHook.class);

    public static final String[] CONFIG_NAME = {"status", "app-status"};
    public static Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME[0]);
    public static final Map<String, Object> appStatusConfig = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME[1]);

    @Override
    public void onStartup() {
        if (appStatusConfig == null) {
            return;
        }
        List<String> repeatStatusList = new ArrayList<>();
        for (String key : appStatusConfig.keySet()) {
            if (config.containsKey(key)) {
                repeatStatusList.add(key);
                if (logger.isInfoEnabled()) {
                    logger.error("The status code: " + key + " has already in use by light-4j and cannot be overwritten," +
                            " please change to another status code in app-status.yml if necessary.");
                }
            } else if (repeatStatusList.isEmpty()) {
                config.put(key, appStatusConfig.get(key));
            }
        }
        if (!repeatStatusList.isEmpty()) {
            throw new RuntimeException("The status codes: " + repeatStatusList.toString() + " in status.yml and app-status.yml are duplicated.");
        }
    }
}
