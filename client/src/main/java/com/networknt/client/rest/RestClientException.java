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

package com.networknt.client.rest;


import com.networknt.exception.ClientException;

public class RestClientException extends ClientException {

    /**
     * Construct a new instance of {@code RestClientException} with the given message.
     * @param msg the message
     */
    public RestClientException(String msg) {
        super(msg);
    }

    /**
     * Construct a new instance of {@code RestClientException} with the given message and
     * exception.
     * @param msg the message
     * @param ex the exception
     */
    public RestClientException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
