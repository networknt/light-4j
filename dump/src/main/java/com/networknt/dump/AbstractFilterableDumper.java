package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractFilterableDumper extends AbstractDumper {
    //after loadFilterConfig(), filter won't be empty
    protected List<String> filter;

    public AbstractFilterableDumper(Object parentConfig, HttpServerExchange exchange, HttpMessageType type) {
        super(parentConfig, exchange, type);
    }

    protected void loadFilterConfig(String filterOptionName) {
        //init filter with an empty list
        this.filter = new ArrayList<>();
        if (parentConfig instanceof Map) {
            Object filterList = ((Map) parentConfig).get(filterOptionName);
            if(filterList instanceof List<?>) {
                this.filter = (List<String>) filterList;
            }
        }
    }
}
