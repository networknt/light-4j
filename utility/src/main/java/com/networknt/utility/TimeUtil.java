package com.networknt.utility;

import java.util.concurrent.TimeUnit;

public class TimeUtil {
    /**
     * Return the number of milliseconds per time unit.
     *
     * @param timeUnit TimeUnit
     * @return long
     */
    public static long oneTimeUnitMillisecond(TimeUnit timeUnit) {
        long millisecond = 0;
        switch (timeUnit) {
            case MILLISECONDS:
                millisecond = 1;
                break;

            case SECONDS:
                millisecond = 1000;
                break;

            case MINUTES:
                millisecond = 60000;
                break;

            case HOURS:
                millisecond = 3600000;
                break;

            case DAYS:
                millisecond = 86400000;
                break;
        }
        return millisecond;
    }
}
