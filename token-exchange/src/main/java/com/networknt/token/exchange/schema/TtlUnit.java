package com.networknt.token.exchange.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.TimeUnit;

public enum TtlUnit {

    @JsonProperty("nanosecond")
    @JsonAlias({"nano", "n", "Nano", "Nanosecond"})
    NANOSECOND,

    @JsonProperty("microsecond")
    @JsonAlias({"micro", "us", "Micro", "Microsecond"})
    MICROSECOND,

    @JsonProperty("millisecond")
    @JsonAlias({"milli", "ms", "msec", "Millisecond", "Milli"})
    MILLISECOND,

    @JsonProperty("second")
    @JsonAlias({"sec", "s", "Second", "Sec"})
    SECOND,

    @JsonProperty("minute")
    @JsonAlias({"min", "m", "Minute", "Min"})
    MINUTE,

    @JsonProperty("hour")
    @JsonAlias({"hr", "h", "Hour", "Hr"})
    HOUR,

    @JsonProperty("day")
    @JsonAlias({"d", "Day"})
    DAY;

    public long unitToMillis(final long unitTime) {
        return switch (this) {
            case NANOSECOND -> TimeUnit.NANOSECONDS.toMillis(unitTime);
            case MICROSECOND -> TimeUnit.MICROSECONDS.toMillis(unitTime);
            case MILLISECOND -> unitTime;
            case SECOND -> TimeUnit.SECONDS.toMillis(unitTime);
            case MINUTE -> TimeUnit.MINUTES.toMillis(unitTime);
            case HOUR -> TimeUnit.HOURS.toMillis(unitTime);
            case DAY -> TimeUnit.DAYS.toMillis(unitTime);
        };
    }

    public long millisToUnit(final long millisTime) {
        return switch (this) {
            case NANOSECOND -> TimeUnit.MILLISECONDS.toNanos(millisTime);
            case MICROSECOND -> TimeUnit.MILLISECONDS.toMicros(millisTime);
            case MILLISECOND -> millisTime;
            case SECOND -> TimeUnit.MILLISECONDS.toSeconds(millisTime);
            case MINUTE -> TimeUnit.MILLISECONDS.toMinutes(millisTime);
            case HOUR -> TimeUnit.MILLISECONDS.toHours(millisTime);
            case DAY -> TimeUnit.MILLISECONDS.toDays(millisTime);
        };
    }
}
