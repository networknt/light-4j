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

/** Safe operational summary that contains no replay keys or owner tokens. */
public record ReplayStoreSummary(String implementation, ReplayStoreScope scope, long entries, long capacity) {
    public ReplayStoreSummary {
        if (implementation == null || implementation.isBlank() || scope == null || entries < -1 || capacity < -1)
            throw new IllegalArgumentException("Invalid replay store summary.");
    }
}
