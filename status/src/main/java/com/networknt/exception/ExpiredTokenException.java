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

/**
 * This is a checked exception used by JWT verification and it is thrown if
 * the JWT token is expired. Used by Security module only to work around Jose4j
 * only return InvalidTokenException without differentiating expiry or incorrect
 * signature. The security module needs to respond differently to consumer in
 * case of token expiry so that caller will try to renew the token reactively.
 *
 * @author Steve Hu
 */
public class ExpiredTokenException extends Exception {
    private static final long serialVersionUID = 1L;

    public ExpiredTokenException() {
        super();
    }

    public ExpiredTokenException(String message) {
        super(message);
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpiredTokenException(Throwable cause) {
        super(cause);
    }

}
