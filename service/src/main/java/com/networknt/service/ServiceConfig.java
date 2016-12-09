package com.networknt.service;

import java.util.List;
import java.util.Map;

/**
 * Created by steve on 2016-11-26.
 */
public class ServiceConfig {
    String description;
    List<Map<String, List<Object>>> singletons;

    public ServiceConfig() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Map<String, List<Object>>> getSingletons() {
        return singletons;
    }

    public void setSingletons(List<Map<String, List<Object>>> singletons) {
        this.singletons = singletons;
    }
}
