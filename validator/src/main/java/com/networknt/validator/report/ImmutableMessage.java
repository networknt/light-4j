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

package com.networknt.validator.report;

import static java.util.Objects.requireNonNull;

class ImmutableMessage implements ValidationReport.Message {

    private final String key;
    private final String message;

    ImmutableMessage(final String key, final String message) {
        this.key = requireNonNull(key, "A key is required");
        this.message = requireNonNull(message, "A message is required");
    }

    @Override
    public String getKey() {
        return key;
    }


    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message.replace("\n", "\n\t");
    }

}
