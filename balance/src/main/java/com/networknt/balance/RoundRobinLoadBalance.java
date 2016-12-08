package com.networknt.balance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by stevehu on 2016-12-07.
 */
public class RoundRobinLoadBalance implements LoadBalance {
    static Logger logger = LoggerFactory.getLogger(RoundRobinLoadBalance.class);

    private AtomicInteger idx = new AtomicInteger(0);

    public URL select(List<URL> urls) {
        URL url = null;
        if (urls.size() > 1) {
            url = doSelect(urls);

        } else if (urls.size() == 1) {
            url = urls.get(0);
        }
        return url;
    }

    private URL doSelect(List<URL> urls) {
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

    /**
     * return positive int value of originValue
     * @param originValue
     * @return positive int
     */
    public static int getPositive(int originValue){
        return 0x7fffffff & originValue;
    }

}
