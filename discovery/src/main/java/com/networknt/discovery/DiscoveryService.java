package com.networknt.discovery;

import java.net.URL;
import java.util.List;

/**
 * Created by stevehu on 2016-12-04.
 */
public interface DiscoveryService {

    void subscribe(String serviceName, Notifier notifier);

    void unsubscribe(String serviceName, Notifier notifier);

    List<URL> discover(String serviceName);
}
