package com.networknt.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by stevehu on 2016-12-04.
 */
public abstract class AbstractDiscovery implements DiscoveryService {
    static Logger logger = LoggerFactory.getLogger(AbstractDiscovery.class);

    private ConcurrentHashMap<URL, Map<String, List<URL>>> subscribedCategoryResponses =
            new ConcurrentHashMap<URL, Map<String, List<URL>>>();

    private URL registryUrl;

    public AbstractDiscovery(URL registryUrl) {
        this.registryUrl = registryUrl;
    }

    @Override
    public void subscribe(URL url, Notifier notifier) {
        if (url == null || notifier == null) {
            logger.error("Subscribe with malformed param, url:{}, listener:{}", url, notifier);
            return;
        }
        logger.info("Listener ({}) will subscribe to url ({}) in Registry [{}]", notifier, url, registryUrl);
        doSubscribe(url, notifier);
    }

    @Override
    public void unsubscribe(URL url, Notifier notifier) {
        if (url == null || notifier == null) {
            logger.error("Unsubscribe with malformed param, url:{}, listener:{}", url, notifier);
            return;
        }
        logger.info("Listener ({}) will unsubscribe from url ({}) in Registry [{}]", notifier, url, registryUrl);
        doUnsubscribe(url, notifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URL> discover(URL url) {
        if (url == null) {
            logger.warn("Discover with malformed param, url is null");
            return Collections.emptyList();
        }

        List<URL> results = new ArrayList<URL>();

        Map<String, List<URL>> categoryUrls = subscribedCategoryResponses.get(url);
        if (categoryUrls != null && categoryUrls.size() > 0) {
            for (List<URL> urls : categoryUrls.values()) {
                for (URL tempUrl : urls) {
                    results.add(tempUrl);
                }
            }
        } else {
            List<URL> urlsDiscovered = doDiscover(url);
            if (urlsDiscovered != null) {
                for (URL u : urlsDiscovered) {
                    results.add(u);
                }
            }
        }
        return results;
    }

    protected abstract void doSubscribe(URL url, Notifier notifier);

    protected abstract void doUnsubscribe(URL url, Notifier notifier);

    protected abstract List<URL> doDiscover(URL url);

}
