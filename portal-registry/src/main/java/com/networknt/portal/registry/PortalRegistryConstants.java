package com.networknt.portal.registry;

public class PortalRegistryConstants {
    /**
     * Service Check Interval 10 seconds
     */
    public static int INTERVAL = 10000;

    /**
     * Service Time To Live in second. If there is no heart beat with TTL, the service
     * will be marked as unavailable. Default to 10 seconds
     */
    public static int TTL = INTERVAL;

    /**
     * Heart beat circle，2/3 of ttl
     */
    public static int HEARTBEAT_CIRCLE = (TTL * 2) / 3;

    /**
     * Maximum continuous switch checks, send heart beat is this number is exceeded.
     */
    public static int MAX_SWITCHER_CHECK_TIMES = 10;

    /**
     * Switcher change rate
     */
    public static int SWITCHER_CHECK_CIRCLE = HEARTBEAT_CIRCLE / MAX_SWITCHER_CHECK_TIMES;

    /**
     * portal controller service lookup interval in millisecond
     */
    public static int DEFAULT_LOOKUP_INTERVAL = 30000;
}
