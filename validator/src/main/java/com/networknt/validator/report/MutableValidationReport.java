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

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Simple container for validation messages to allow as much validation information to be collected as possible
 */
public class MutableValidationReport implements ValidationReport {

    private final List<ValidationReport.Message> messages = new ArrayList<>();

    /**
     * Add a validation message to this report.
     *
     * @param message The validation message to include
     *
     * @return This validation report instance
     */
    public MutableValidationReport add(final Message message) {
        if (message != null) {
            this.messages.add(message);
        }
        return this;
    }

    public void addAll(final ValidationReport other) {
        this.messages.addAll(other.getMessages());
    }

    @Override
    public ValidationReport merge(final ValidationReport other) {
        requireNonNull(other, "A validation report is required");

        this.messages.addAll(this.getMessages());
        this.messages.addAll(other.getMessages());
        return this;
    }

    @Override
    public List<ValidationReport.Message> getMessages() {
        return unmodifiableList(messages);
    }
}
