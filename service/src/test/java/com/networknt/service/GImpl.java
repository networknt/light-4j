package com.networknt.service;

/**
 * Created by stevehu on 2016-11-29.
 */
public class GImpl implements G {
    String name;
    int age;

    public GImpl() {
    }

    public GImpl(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String getName() {
        return name;
    }
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void setAge(int age) {
        this.age = age;
    }
}
