package com.networknt.service;

public interface Validator<T> {
    boolean validate(T object);
}
