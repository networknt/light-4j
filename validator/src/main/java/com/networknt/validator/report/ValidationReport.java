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

import java.util.Collections;
import java.util.List;

/**
 * A report of validation errors that occurred during validation.
 * <p>
 */
public interface ValidationReport {

    /**
     * A single message in the validation report
     */
    interface Message {
        String getKey();
        String getMessage();
    }

    ValidationReport EMPTY_REPORT = new ValidationReport(){

        @Override
        public boolean hasErrors() {
            return false;
        }

        @Override
        public List<Message> getMessages() {
            return Collections.emptyList();
        }

        @Override
        public ValidationReport merge(ValidationReport other) {
            return other;
        }
    };

    /**
     * Return an empty report.
     *
     * @return an immutable empty report
     */
    static ValidationReport empty() {
        return EMPTY_REPORT;
    }

    /**
     * Return an unmodifiable report that contains a single message.
     *
     * @param message The message to add to the report
     *
     * @return An unmodifiable validation report with a single message
     */
    static ValidationReport singleton(final Message message) {
        if (message == null) {
            return empty();
        }

        return new ValidationReport() {

            @Override
            public boolean hasErrors() {
                return true;
            }

            @Override
            public List<Message> getMessages() {
                return Collections.singletonList(message);
            }

            @Override
            public ValidationReport merge(ValidationReport other) {
                final MutableValidationReport result = new MutableValidationReport();
                result.addAll(this);
                result.addAll(other);
                return result;
            }
        };
    }

    /**
     * Return if this validation report contains errors.
     *
     * @return <code>true</code> if a validation error exists; <code>false</code> otherwise.
     */
    default boolean hasErrors() {
        return getMessages().size() > 0;
    }

    /**
     * Get the validation messages on this report.
     *
     * @return The messages recorded on this report
     */
    List<Message> getMessages();

    /**
     * Merge the validation messages from the given report with this one, and return a
     * new report with the merged messaged.
     *
     * @param other The validation report to merge with this one
     *
     * @return A new report that contains all the messages from this report and the other report
     */
    ValidationReport merge(ValidationReport other);

}
