package com.networknt.registry;

import java.net.URL;

/**
 * Created by steve on 2016-12-04.
 */
public interface RegistryService {

    void register(String serviceName, URL url);

    void unregister(String serviceName, URL url);

    void available(String serviceName, URL url);

    void unavailable(String serviceName, URL url);

}
