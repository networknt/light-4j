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
	 * Default consul agent port
	 */
	public static int DEFAULT_PORT = 8500;

    /**
     * Service Check Interval
     */
    public static String INTERVAL = ConsulService.config.checkInterval == null ? "10s" : ConsulService.config.checkInterval;

    /**
     * Service TCP Check Deregister After
     */
    public static String DEREGISTER_AFTER = "2m";

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
