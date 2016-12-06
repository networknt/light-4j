package com.networknt.discovery;

import java.net.URL;
import java.util.List;

/**
 * Created by stevehu on 2016-12-04.
 */
public interface DiscoveryService {

    void subscribe(URL url, Notifier notifier);

    void unsubscribe(URL url, Notifier notifier);

    List<URL> discover(URL url);
}
