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

package com.networknt.hmac;

/** Safe provider failure surfaced to the HMAC handler as HTTP 503. */
public final class ReplayStoreUnavailableException extends RuntimeException {
    public enum Reason { CAPACITY, PROVIDER }

    private final Reason reason;

    public ReplayStoreUnavailableException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ReplayStoreUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
