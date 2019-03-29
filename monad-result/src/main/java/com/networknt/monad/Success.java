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
