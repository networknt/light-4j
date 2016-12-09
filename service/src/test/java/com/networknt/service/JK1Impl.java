package com.networknt.service;

/**
 * Created by steve on 2016-11-29.
 */
public class JK1Impl implements J, K {
    String jack;
    String king;

    public JK1Impl() {
    }

    @Override
    public String getJack() {
        return jack;
    }

    @Override
    public void setJack(String jack) {
        this.jack = jack;
    }

    @Override
    public String getKing() {
        return king;
    }

    @Override
    public void setKing(String king) {
        this.king = king;
    }
}
