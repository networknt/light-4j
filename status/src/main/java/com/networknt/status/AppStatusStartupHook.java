package com.networknt.status;

import com.networknt.config.Config;
import com.networknt.server.StartupHookProvider;

import java.util.Map;

/**
 * The startup hook is used to merge status.yml and app-status.yml
 * <p>
 * 1. If repeated elements is contained by these two files, throw a RuntimeException when startup.
 * 2. If elements contained by app-status.yml is new, append them to status.yml
 * <p>
 * To implement this startup hook, uncomment it in service.yml
 * @author Jiachen Sun
 */
public class AppStatusStartupHook implements StartupHookProvider {
    public static final String[] CONFIG_NAME = {"status", "app-status"};
    public static Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME[0]);
    public static final Map<String, Object> appStatusConfig = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME[1]);

    @Override
    public void onStartup() {
        if (appStatusConfig == null) {
            return;
        }
        for (String key : appStatusConfig.keySet()) {
            if (config.containsKey(key)) {
                System.out.println("The status code: " + key + " has already in use by light-4j and cannot be overwritten," +
                        " please change to another status code in app-status.yml if necessary.");
                throw new RuntimeException("The status codes in status.yml and app-status.yml are repeated.");
            } else {
                config.put(key, appStatusConfig.get(key));
            }
        }
    }
}
