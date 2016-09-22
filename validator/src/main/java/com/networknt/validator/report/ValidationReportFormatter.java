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

/**
 * Format a {@link ValidationReport} instance in a nice String representation for use in e.g. logs or exceptions.
 */
public class ValidationReportFormatter {

    /**
     * Format the given report in a nice String representation
     *
     * @param report The report to format
     *
     * @return A String representation of the given report
     */
    public static String format(final ValidationReport report) {
        if (report == null) {
            return "Validation report is null.";
        }
        final StringBuilder b = new StringBuilder();
        if (!report.hasErrors()) {
            b.append("No validation errors.");
        } else {
            b.append("Validation failed.");
        }
        report.getMessages().forEach(m -> b.append("\n[").append("] ").append(m.getMessage().replace("\n", "\n\t")));
        return b.toString();
    }

}
