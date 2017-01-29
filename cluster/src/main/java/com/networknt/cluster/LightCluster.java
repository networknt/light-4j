package com.networknt.cluster;

import com.networknt.balance.LoadBalance;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by stevehu on 2017-01-27.
 */
public class LightCluster implements Cluster {
    private static Logger logger = LoggerFactory.getLogger(LightCluster.class);
    private static Registry registry = (Registry) SingletonServiceFactory.getBean(Registry.class);
    private static LoadBalance loadBalance = (LoadBalance)SingletonServiceFactory.getBean(LoadBalance.class);

    public LightCluster() {
        if(logger.isInfoEnabled()) logger.info("A LightCluster instance is started");
    }

    @Override
    public String serviceToUrl(String protocol, String serviceId) {
        if(logger.isDebugEnabled()) logger.debug("protocol = " + protocol + " serviceId = " + serviceId);
        URL subscribeUrl = URLImpl.valueOf("light://localhost/" + serviceId);
        if(logger.isDebugEnabled()) logger.debug("subscribeUrl = " + subscribeUrl);
        List<URL> urls = registry.discover(subscribeUrl);
        if(logger.isDebugEnabled()) logger.debug("discovered urls = " + urls);
        URL url = loadBalance.select(urls);
        if(logger.isDebugEnabled()) logger.debug("final url after load balance = " + url);
        // construct a url in string
        return protocol + "://" + url.getHost() + ":" + url.getPort();
    }
}
