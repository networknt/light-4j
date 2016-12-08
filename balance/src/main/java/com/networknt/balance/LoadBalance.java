package com.networknt.balance;

import java.net.URL;
import java.util.List;

public interface LoadBalance {
    // select one from a list of URLs
    URL select(List<URL> urls);
}
