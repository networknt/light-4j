package com.networknt.service;

import java.util.List;

/**
 * Created by stevehu on 2016-11-26.
 */
public class ServiceConfig {
    String description;
    List<String> singletons;

    public ServiceConfig() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSingletons() {
        return singletons;
    }

    public void setSingletons(List<String> singletons) {
        this.singletons = singletons;
    }
}
