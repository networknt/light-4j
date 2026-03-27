/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.registry;

import com.networknt.utility.Constants;

/**
 * The URL parameters are extra information attached to URL which is the main
 * object for service registry and discovery. This class defines all types of
 * URL parameters that are supported in the framework.
 *
 * @author maijunsheng, Steve Hu
 *
 */
public enum URLParamType {
    /** environment **/
    environment("environment", "production"),
    /** version **/
    version("version", Constants.DEFAULT_VERSION),
    /** request timeout **/
    requestTimeout("requestTimeout", 200),
    /** request id from http interface **/
    requestIdFromClient("requestIdFromClient", 0),
    /** connect timeout **/
    connectTimeout("connectTimeout", 1000),
    /** service min worker threads **/
    minWorkerThread("minWorkerThread", 20),
    /** service max worker threads **/
    maxWorkerThread("maxWorkerThread", 200),
    /** pool min conn number **/
    minClientConnection("minClientConnection", 2),
    /** pool max conn number **/
    maxClientConnection("maxClientConnection", 10),
    /** pool max conn number **/
    maxContentLength("maxContentLength", 10 * 1024 * 1024),
    /** max server conn (all clients conn) **/
    maxServerConnection("maxServerConnection", 100000),
    /** pool conn manager strategy **/
    poolLifo("poolLifo", true),

    /** lazy init */
    lazyInit("lazyInit", false),

    /** multi referer share the same channel **/
    shareChannel("shareChannel", false),

    /** serialize **/
    serialize("serialization", "hessian2"),
    /** codec **/
    codec("codec", "light"),
    /** endpointFactory **/
    endpointFactory("endpointFactory", "light"),
    /** heartbeatFactory **/
    heartbeatFactory("heartbeatFactory", "light"),
    /** switcherService **/
    switcherService("switcherService", "localSwitcherService"),

    /** group */
    group("group", "default"),
    /** client group */
    clientGroup("clientGroup", "default"),
    /** access log */
    accessLog("accessLog", false),

    /** actives */
    actives("actives", 0),

    /** refresh timestamp */
    refreshTimestamp("refreshTimestamp", 0),
    /** node type */
    nodeType("nodeType", Constants.NODE_TYPE_SERVICE),

    /** export */
    export("export", ""),
    /** embed */
    embed("embed", ""),

    /** registry retry period */
    registryRetryPeriod("registryRetryPeriod", 30 * Constants.SECOND_MILLS),

    /** cluster */
    cluster("cluster", Constants.DEFAULT_VALUE),
    /** load balance */
    loadbalance("loadbalance", "activeWeight"),
    /** HA strategy */
    haStrategy("haStrategy", "failover"),
    /** protocol */
    protocol("protocol", Constants.PROTOCOL_LIGHT),
    /** path */
    path("path", ""),
    /** host */
    host("host", ""),
    /** port */
    port("port", 0),
    /** IO threads */
    iothreads("iothreads", Runtime.getRuntime().availableProcessors() + 1),
    /** worker queue size */
    workerQueueSize("workerQueueSize", 0),
    /** accept connections */
    acceptConnections("acceptConnections", 0),
    /** filter */
    filter("filter", ""),

    /** application */
    application("application", Constants.FRAMEWORK_NAME),
    /** module */
    module("module", Constants.FRAMEWORK_NAME),

    /** retries */
    retries("retries", 0),
    /** async */
    async("async", false),
    /** mock */
    mock("mock", "false"),
    /** mean */
    mean("mean", "2"),
    /** p90 */
    p90("p90", "4"),
    /** p99 */
    p99("p99", "10"),
    /** p999 */
    p999("p999", "70"),
    /** error rate */
    errorRate("errorRate", "0.01"),
    /** check */
    check("check", "true"),
    /** direct URL */
    directUrl("directUrl", ""),
    /** registry session timeout */
    registrySessionTimeout("registrySessionTimeout", 1 * Constants.MINUTE_MILLS),

    /** register */
    register("register", true),
    /** subscribe */
    subscribe("subscribe", true),
    /** throw exception */
    throwException("throwException", "true"),

    /** local service address */
    localServiceAddress("localServiceAddress", ""),

    /** weights */
    weights("weights", "");

    private final String name;
    private final String value;
    private final long longValue;
    private final int intValue;
    private final boolean boolValue;

    private URLParamType(String name, String value) {
        this.name = name;
        this.value = value;
        this.longValue = 0L;
        this.intValue = 0;
        this.boolValue = false;

    }

    private URLParamType(String name, long longValue) {
        this.name = name;
        this.value = String.valueOf(longValue);
        this.longValue = longValue;
        this.intValue = 0;
        this.boolValue = false;
    }

    private URLParamType(String name, int intValue) {
        this.name = name;
        this.value = String.valueOf(intValue);
        this.intValue = intValue;
        this.longValue = 0L;
        this.boolValue = false;

    }

    private URLParamType(String name, boolean boolValue) {
        this.name = name;
        this.value = String.valueOf(boolValue);
        this.boolValue = boolValue;
        this.longValue = 0L;
        this.intValue = 0;
    }

    /**
     * Returns the name of the parameter.
     *
     * @return String name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the string value of the parameter.
     *
     * @return String value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the integer value of the parameter.
     *
     * @return int value
     */
    public int getIntValue() {
        return intValue;
    }

    /**
     * Returns the long value of the parameter.
     *
     * @return long value
     */
    public long getLongValue() {
        return longValue;
    }

    /**
     * Returns the boolean value of the parameter.
     *
     * @return boolean value
     */
    public boolean getBooleanValue() {
        return boolValue;
    }

}
