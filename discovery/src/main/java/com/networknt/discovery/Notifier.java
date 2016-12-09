package com.networknt.discovery;

import java.net.URL;
import java.util.List;

/**
 * Created by steve on 2016-12-04.
 */
public interface Notifier {
    void notify(String serviceName, List<URL> urls);
}
