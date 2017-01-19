package com.networknt.service;

/**
 * Created by stevehu on 2017-01-18.
 */
public class MImpl implements M {
    String name;
    int value = 0;

    public MImpl(String name, int v1, int v2) {
        this.name = name;
        value = v1 + v2;
    }

    public int getValue() {
        return value;
    }
}
