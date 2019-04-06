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

package com.networknt.utility;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class DateUtil {

    /** Alternate ISO 8601 format without fractional seconds. */
    static final DateTimeFormatter ALTERNATE_ISO_8601_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .toFormatter()
                    .withZone(UTC);

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date the date to format
     * @return the ISO-8601 string representing the specified date
     */
    public static String formatIso8601Date(Instant date) {
        return ISO_INSTANT.format(date);
    }

    /**
     * Parses the specified date string as an ISO 8601 date (yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
     * and returns the {@link Instant} object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Instant object.
     */
    public static Instant parseIso8601Date(String dateString) {
        // For EC2 Spot Fleet.
        if (dateString.endsWith("+0000")) {
            dateString = dateString
                    .substring(0, dateString.length() - 5)
                    .concat("Z");
        }

        try {
            return parseInstant(dateString, ISO_INSTANT);
        } catch (DateTimeParseException e) {
            return parseInstant(dateString, ALTERNATE_ISO_8601_DATE_FORMAT);
        }
    }

    private static Instant parseInstant(String dateString, DateTimeFormatter formatter) {
        return formatter.withZone(ZoneOffset.UTC).parse(dateString, Instant::from);
    }

}
