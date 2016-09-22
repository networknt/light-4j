/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.utility;

/**
 * Created by steve on 02/09/16.
 */
public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;
    private ErrorResponse errorResponse;

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public ApiException(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ApiException(int code, String message) {
        super(message);
        this.errorResponse = new ErrorResponse(code, message);
    }

    public ApiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.errorResponse = new ErrorResponse(code, message);
    }

    public ApiException(ErrorResponse errorResponse, Throwable cause) {
        super(cause);
        this.errorResponse = errorResponse; }
}
