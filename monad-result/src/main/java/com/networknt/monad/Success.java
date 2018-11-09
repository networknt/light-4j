package com.networknt.monad;

import com.networknt.status.Status;

import java.util.NoSuchElementException;
import java.util.Optional;

public final class Success<T> implements Result<T> {

    public static final Result<Void> SUCCESS = new Success<>(null);

    public static final Result OPTIONAL_SUCCESS = Success.ofOptional(null);

    @SuppressWarnings("unchecked")
    static <T> Result<Optional<T>> emptyOptional() {
        return (Result<Optional<T>>) OPTIONAL_SUCCESS;
    }

    private final T result;

    public static <T> Result<T> of(T result) {
        return new Success<>(result);
    }

    public static <T> Result<Optional<T>> ofOptional(T result) {
        return new Success<>(Optional.ofNullable(result));
    }

    private Success(T result) {
        this.result = result;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public Status getError() {
        throw new NoSuchElementException("There is no error in Success");
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public String toString() {
        final String value = result != null ? result.toString() : "";
        return String.format("Success[%s]", value);
    }
}
