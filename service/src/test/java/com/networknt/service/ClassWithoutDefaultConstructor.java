package com.networknt.service;

/**
 * @author Nicholas Azar
 */
public class ClassWithoutDefaultConstructor {
    private String first;
    private String second;
    public ClassWithoutDefaultConstructor(String first, String second) {
        this.first = first;
        this.second = second;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }
}
