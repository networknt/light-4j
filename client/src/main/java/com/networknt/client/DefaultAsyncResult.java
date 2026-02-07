/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client;

/**
 * A default implementation of the {@link AsyncResult} interface.
 *
 * @param <T> the type of the result
 */
public class DefaultAsyncResult<T> implements AsyncResult<T> {

    private Throwable cause;
    private T result;

    /**
     * @deprecated should be created use {@link #succeed(Object)} or {@link #fail(Throwable)}
     * @param cause Throwable throwable exceptions
     * @param result result
     */
    @Deprecated
    public DefaultAsyncResult(Throwable cause, T result) {
        this.cause = cause;
        this.result = result;
    }

    /**
     * Creates a successful AsyncResult with the given result.
     * @param <T> the type of the result
     * @param result the result of the operation
     * @return a successful AsyncResult
     */
    public static <T> AsyncResult<T> succeed(T result) {
        return new DefaultAsyncResult<>(null, result);
    }

    /**
     * Creates a successful AsyncResult with a null result.
     * @return a successful AsyncResult
     */
    public static AsyncResult<Void> succeed() {
        return succeed(null);
    }

    /**
     * Creates a failed AsyncResult with the given cause.
     * @param <T> the type of the result
     * @param cause the cause of failure
     * @return a failed AsyncResult
     */
    public static <T> AsyncResult<T> fail(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("cause argument cannot be null");
        }

        return new DefaultAsyncResult<>(cause, null);
    }

    /**
     * Creates a failed AsyncResult using the cause from another AsyncResult.
     * @param <T> the type of the result
     * @param result the AsyncResult to get the cause from
     * @return a failed AsyncResult
     */
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
