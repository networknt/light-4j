package com.networknt.service;

public class OImpl implements O
{
    private String _val;

    public OImpl(String val) {
        _val = val;
    }

    @Override
    public String getVal() {
        return _val;
    }
}
