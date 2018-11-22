package com.networknt.service;

public class NImpl implements N
{
    private String _val;

    public NImpl(String val) {
        _val = val;
    }

    @Override
    public String getVal() {
        return _val;
    }
}
