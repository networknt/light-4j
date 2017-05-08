package com.networknt.balance;

import com.networknt.registry.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round Robin Loadbalance will pick up a url from a list of urls one by one
 * for each call. It will distributed the load equally to all urls in the list.
 *
 * This class has an instance variable called idx which is AtomicInteger and it
 * increases for every select call to make sure all urls in the list will have
 * an opportunity to be selected.
 *
 * The assumption for round robin is based on all service will have the same
 * hardware/cloud resource configuration so that they can be treated as the
 * same priority.
 *
 * Created by steve on 2016-12-07.
 */
public class RoundRobinLoadBalance implements LoadBalance {
    static Logger logger = LoggerFactory.getLogger(RoundRobinLoadBalance.class);

    public RoundRobinLoadBalance() {
        if(logger.isInfoEnabled()) logger.info("A RoundRobinLoadBalance instance is started");
    }

    private AtomicInteger idx = new AtomicInteger(0);

    /**
     * Round robin requestKey is not used as it should be null, the url will
     * be selected from the list base on an instance idx so every url has the
     * same priority.
     *
     * @param urls List
     * @param requestKey String
     * @return
     */
    @Override
    public URL select(List<URL> urls, String requestKey) {
        URL url = null;
        if (urls.size() > 1) {
            url = doSelect(urls);

        } else if (urls.size() == 1) {
            url = urls.get(0);
        }
        return url;
    }

    protected URL doSelect(List<URL> urls) {
        int index = getNextPositive();
        for (int i = 0; i < urls.size(); i++) {
            URL url = urls.get((i + index) % urls.size());
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    // get positive int
    private int getNextPositive() {
        return getPositive(idx.incrementAndGet());
    }


}
