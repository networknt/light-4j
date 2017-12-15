package com.networknt.client;

public class DefaultAsyncResult<T> implements AsyncResult<T> {

    private Throwable cause;
    private T result;

    public DefaultAsyncResult(Throwable cause, T result) {
        this.cause = cause;
        this.result = result;
    }

    public static <T> AsyncResult<T> succeed(T result) {
        return new DefaultAsyncResult<>(null, result);
    }

    public static AsyncResult<Void> succeed() {
        return succeed(null);
    }

    public static <T> AsyncResult<T> fail(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("cause argument cannot be null");
        }

        return new DefaultAsyncResult<>(cause, null);
    }

    public static <T> AsyncResult<T> fail(AsyncResult<?> result) {
        return fail(result.cause());
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean succeeded() {
        return cause == null;
    }

    @Override
    public boolean failed() {
        return cause != null;
    }
}
