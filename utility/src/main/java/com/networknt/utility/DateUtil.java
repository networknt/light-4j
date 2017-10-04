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
