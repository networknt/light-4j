package com.networknt.service;

public class NFactory {
    private String _val = "default";

    public NFactory() {}

    public NFactory(String val) {
        _val = val != null ? val : _val;
    }

    public NImpl getN() {
        return new NImpl(_val);
    }
}
