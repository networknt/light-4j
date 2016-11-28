package com.networknt.service;

/**
 * Created by stevehu on 2016-11-27.
 */
public class CImpl implements C {
    A a;
    B b;

    public CImpl(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public String c() {
        return a.a() + b.b();
    }
}
