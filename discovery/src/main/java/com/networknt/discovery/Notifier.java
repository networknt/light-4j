package com.networknt.discovery;

import java.net.URL;
import java.util.List;

/**
 * Created by stevehu on 2016-12-04.
 */
public interface Notifier {
    void notify(URL url, List<URL> urls);
}
