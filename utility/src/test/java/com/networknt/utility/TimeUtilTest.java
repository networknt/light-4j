package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimeUtilTest {
    @Test
    public void testNextSecond() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUtil.nextStartTimestamp(TimeUnit.SECONDS, start);
        System.out.println("next second = " + nextTimestamp);
    }

    @Test
    public void testNextMinute() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUtil.nextStartTimestamp(TimeUnit.MINUTES, start);
        System.out.println("next minute = " + nextTimestamp);
    }

    @Test
    public void testNextHour() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUtil.nextStartTimestamp(TimeUnit.HOURS, start);
        System.out.println("next hour = " + nextTimestamp);
    }

    @Test
    public void testNextDay() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUtil.nextStartTimestamp(TimeUnit.DAYS, start);
        System.out.println("next day = " + nextTimestamp);
    }

    @Test
    public void testTimeUnitToMillisecond() {
        Assert.assertEquals(1000, TimeUtil.oneTimeUnitMillisecond(TimeUnit.SECONDS));
    }
}
