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
 * This is a checked exception that can wrap Status object to give consume a uniform
 * response. It is recommended that the business handler to catch this exception and
 * respond to the request; however, the ExceptionHandler in this module will capture
 * it and translate into a meaningful error response.
 *
 * Note that ExceptionHandler is a middleware handler and it is plugged in by default
 * and can be turned off via configuration.
 *
 * @author Steve Hu
 */
public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Status status;

    public Status getStatus() {
        return status;
    }

    public ApiException(Status status) {
        super(status.toString());
        this.status = status;
    }

    public ApiException(Status status, Throwable cause) {
        super(status.toString(), cause);
        this.status = status;
    }
}
