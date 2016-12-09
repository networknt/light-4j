package com.networknt.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by steve on 2016-12-04.
 */
public abstract class AbstractDiscovery implements DiscoveryService {
    static final Logger logger = LoggerFactory.getLogger(AbstractDiscovery.class);

    private final ConcurrentHashMap<String, List<URL>> serviceMap =
            new ConcurrentHashMap<>();

    private URL registryUrl;

    public AbstractDiscovery() {

    }

    @Override
    public void subscribe(String serviceName, Notifier notifier) {
        if (serviceName == null || notifier == null) {
            logger.error("Subscribe with malformed param, serviceName:{}, listener:{}", serviceName, notifier);
            return;
        }
        logger.info("Listener ({}) will subscribe to serviceName ({}) in Registry [{}]", notifier, serviceName, registryUrl);
        doSubscribe(serviceName, notifier);
    }

    @Override
    public void unsubscribe(String serviceName, Notifier notifier) {
        if (serviceName == null || notifier == null) {
            logger.error("Unsubscribe with malformed param, serviceName:{}, listener:{}", serviceName, notifier);
            return;
        }
        logger.info("Listener ({}) will unsubscribe from serviceName ({}) in Registry [{}]", notifier, serviceName, registryUrl);
        doUnsubscribe(serviceName, notifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URL> discover(String serviceName) {
        if (serviceName == null) {
            logger.warn("Discover with malformed param, serviceName is null");
            return Collections.emptyList();
        }

        List<URL> services = serviceMap.get(serviceName);
        if (services == null || services.size() == 0) {
            services = doDiscover(serviceName);
            if (services != null) {
                serviceMap.put(serviceName, services);
            }
        }
        return services;
    }

    protected abstract void doSubscribe(String serviceName, Notifier notifier);

    protected abstract void doUnsubscribe(String serviceName, Notifier notifier);

    protected abstract List<URL> doDiscover(String serviceName);

}
