package com.networknt.discovery;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stevehu on 2016-12-06.
 * Registration info is defined in the configuration and static. Usually, it only used on local testing.
 */
public class LocalDiscoveryService extends AbstractDiscovery {
    static Logger logger = LoggerFactory.getLogger(LocalDiscoveryService.class);
    static String CONFIG_NAME = "discovery";

    @Override
    public void doSubscribe(String serviceName, Notifier notifier) {
        logger.info("LocalRegistryService subscribe: serviceName ={}", serviceName);
    }

    @Override
    public void doUnsubscribe(String serviceName, Notifier notifier) {
        logger.info("LocalRegistryService unsubscribe: serviceName ={}", serviceName);
    }

    @Override
    public List<URL> doDiscover(String serviceName) {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
        Map<String, List<String>> map = (Map<String, List<String>>)config.get("serviceMap");
        List<String> strings = map.get(serviceName);
        List<URL> urls = new ArrayList<>();
        for(String s: strings) {
            try {
                URL url = new URL(s);
                urls.add(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return urls;
    }

}
