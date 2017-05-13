package com.networknt.service;

import java.util.List;
import java.util.Map;

/**
 * Service Config Class that encapsulate all the service defined in service.yml
 *
 * @author Steve Hu
 */
public class ServiceConfig {
    List<Map<String, List<Object>>> singletons;

    public ServiceConfig() {
    }

    public List<Map<String, List<Object>>> getSingletons() {
        return singletons;
    }

    public void setSingletons(List<Map<String, List<Object>>> singletons) {
        this.singletons = singletons;
    }
}
