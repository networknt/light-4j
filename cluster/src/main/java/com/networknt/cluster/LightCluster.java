package com.networknt.cluster;

import com.networknt.balance.LoadBalance;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ConcurrentHashSet;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by stevehu on 2017-01-27.
 */
public class LightCluster implements Cluster {
    private static Logger logger = LoggerFactory.getLogger(LightCluster.class);
    private static Registry registry = (Registry) SingletonServiceFactory.getBean(Registry.class);
    private static LoadBalance loadBalance = (LoadBalance)SingletonServiceFactory.getBean(LoadBalance.class);
    private static Set<URL> subscribedSet = new ConcurrentHashSet<>();
    private static Map<String, List<URL>> serviceMap = new ConcurrentHashMap<>();

    public LightCluster() {
        if(logger.isInfoEnabled()) logger.info("A LightCluster instance is started");
    }

    @Override
    public String serviceToUrl(String protocol, String serviceName) {
        if(logger.isDebugEnabled()) logger.debug("protocol = " + protocol + " serviceName = " + serviceName);
        // lookup in serviceMap first, if not there, then subscribe and discover.
        List<URL> urls = serviceMap.get(serviceName);
        if(logger.isDebugEnabled()) logger.debug("cached serviceName " + serviceName + " urls = " + urls);
        if(urls == null) {
            URL subscribeUrl = URLImpl.valueOf("light://localhost/" + serviceName);
            if(logger.isDebugEnabled()) logger.debug("subscribeUrl = " + subscribeUrl);
            // you only need to subscribe once.
            if(!subscribedSet.contains(subscribeUrl)) {
                registry.subscribe(subscribeUrl, new ClusterNotifyListener());
                subscribedSet.add(subscribeUrl);
            }
            urls = registry.discover(subscribeUrl);
            if(logger.isDebugEnabled()) logger.debug("discovered urls = " + urls);
        }
        URL url = loadBalance.select(urls);
        if(logger.isDebugEnabled()) logger.debug("final url after load balance = " + url);
        // construct a url in string
        return protocol + "://" + url.getHost() + ":" + url.getPort();
    }

    static class ClusterNotifyListener implements NotifyListener {
        @Override
        public void notify(URL registryUrl, List<URL> urls) {
            if(logger.isDebugEnabled()) logger.debug("notify is called in ClusterNotifyListener registryUrl = " + registryUrl + " urls = " + urls);
            if(urls != null && urls.size() > 0) serviceMap.put(urls.get(0).getPath(), urls);
        }
    }
}
