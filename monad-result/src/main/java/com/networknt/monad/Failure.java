package com.networknt.monad;

import com.networknt.status.Status;

import java.util.NoSuchElementException;

public final class Failure<T> implements Result<T> {

    private final Status error;

    private Failure(Status error) {
        this.error = error;
    }

    public static <T> Result<T> of(Status error) {
        return new Failure<>(error);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Status getError() {
        return error;
    }

    @Override
    public T getResult() {
        throw new NoSuchElementException("There is no result is Failure");
    }

    @Override
    public String toString() {
        return String.format("Failure[%s]", error.toString());
    }
}
