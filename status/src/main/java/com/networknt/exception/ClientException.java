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

package com.networknt.exception;

import com.networknt.status.Status;

/**
 * This is a checked exception used by Client module.
 *
 * @author Steve Hu
 */
public class ClientException extends Exception {
    private static final long serialVersionUID = 1L;
    /** The status object associated with this exception type */
    private static Status status = new Status();

    /**
     * Default constructor for ClientException.
     */
    public ClientException() {
        super();
    }

    /**
     * Constructs a ClientException with a message.
     *
     * @param message error message
     */
    public ClientException(String message) {
        super(message);
    }

    /**
     * Constructs a ClientException with a status.
     *
     * @param status Status object
     */
    public ClientException(Status status) {
        ClientException.status = status;
    }

    /**
     * Constructs a ClientException with a message and a cause.
     *
     * @param message error message
     * @param cause   Throwable cause
     */
    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a ClientException with a cause.
     *
     * @param cause Throwable cause
     */
    public ClientException(Throwable cause) {
        super(cause);
    }

    /**
     * Gets the status object.
     *
     * @return Status object
     */
    public static Status getStatus() {
        return status;
    }

    /**
     * Sets the status object.
     *
     * @param status Status object
     */
    public static void setStatus(Status status) {
        ClientException.status = status;
    }
}
