package com.networknt.service;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stevehu on 2016-11-26.
 */
public class SingletonServiceFactory {
    static String CONFIG_NAME = "service";
    static Logger logger = LoggerFactory.getLogger(SingletonServiceFactory.class);

    private static Map<Class, Object> serviceMap = new HashMap<>();

    static {
        ServiceConfig serviceConfig =
                (ServiceConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServiceConfig.class);
        List<String> singletons = serviceConfig.getSingletons();
        try {
            if(singletons != null && singletons.size() > 0) {
                for(String singleton: singletons) {
                    int i = singleton.indexOf(':');
                    String interfaceName = singleton.substring(0, i);
                    String implName = singleton.substring(i + 1);
                    Class interfaceClass = Class.forName(interfaceName);
                    Class implClass = Class.forName(implName);
                    serviceMap.put(interfaceClass, implClass.newInstance());

                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public static Object getBean(Class interfaceClass) {
       return serviceMap.get(interfaceClass);
    }
}
