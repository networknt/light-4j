package com.networknt.consul;

public class ConsulConstants {
	/**
	 * Light java protocol prefix in consul tag
	 */
	public static final String CONSUL_TAG_LIGHT_PROTOCOL = "protocol_";
	/**
	 * Light java url prefix in consul tag
	 */
	public static final String CONSUL_TAG_LIGHT_URL = "URL_";

	/**
	 * Light java command in consul
	 *
	 */
	public static final String CONSUL_LIGHT_COMMAND = "light/command/";


	/**
	 * Default protocol
	 */
	public static final String DEFAULT_PROTOCOL = "http";

	/**
	 * Default consul agent port
	 */
	public static int DEFAULT_PORT = 8500;

	/**
	 * Service Time To Live in second. If there is no heart beat with TTL, the service
     * will be marked as unavailable.
	 */
	public static int TTL = 30;

	/**
	 * HEARTBEAT_TTL string format
	 */
	public static String TTL_STR = TTL + "s";

	/**
	 * Service TCP Check Interval
	 */
	public static String INTERVAL = "10s";

	/**
	 * Service TCP Check Deregister After
	 */
	public static String DEREGISTER_AFTER = "90m";

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
	 * consul service lookup interval in millisecond
	 */
	public static int DEFAULT_LOOKUP_INTERVAL = 30000;

	/**
	 * consul heart beat switcher
	 */
	@Deprecated
	public static final String CONSUL_PROCESS_HEARTBEAT_SWITCHER = "feature.consul.heartbeat";

	/**
	 * consul block, max block time in minute
	 */
	public static int CONSUL_BLOCK_TIME_MINUTES = 10;
	
	/**
	 * consul block max block time in second
	 */
	public static long CONSUL_BLOCK_TIME_SECONDS = CONSUL_BLOCK_TIME_MINUTES * 60;

	/**
	 * consul configuration file name
	 */
	public static final String CONFIG_NAME = "consul";

}
