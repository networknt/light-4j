package com.networknt.portal.registry;

public class PortalRegistryConstants {
    /**
     * Service Check Interval
     */
    public static String INTERVAL = PortalRegistryService.config.checkInterval == null ? "10s" : PortalRegistryService.config.checkInterval;

    /**
     * Service Time To Live in second. If there is no heart beat with TTL, the service
     * will be marked as unavailable.
     */
    public static int TTL = Integer.valueOf(INTERVAL.substring(0,INTERVAL.length() - 1));

    /**
     * Heart beat circle，2/3 of ttl
     */
    public static int HEARTBEAT_CIRCLE = (TTL * 1000 * 2) / 3;

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
