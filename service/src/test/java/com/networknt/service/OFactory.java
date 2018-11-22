package com.networknt.service;

public class OFactory {
    private String _val = "default";

    public OFactory() {}

    public OFactory(String val) {
        _val = val != null ? val : _val;
    }

    public OImpl getO() {
        return new OImpl(_val);
    }
}
