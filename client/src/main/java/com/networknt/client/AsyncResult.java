package com.networknt.client;

public interface AsyncResult<T> {

    //The result of the operation. This will be null if the operation failed.
    T result();

    //An exception describing failure. This will be null if the operation succeeded.
    Throwable cause();

    //Did it succeed?
    boolean succeeded();

    //Did it fail?
    boolean failed();
}
