package com.networknt.balance;

import com.networknt.registry.URL;

import java.util.List;

public interface LoadBalance {
    // select one from a list of URLs
    URL select(List<URL> urls);
}
