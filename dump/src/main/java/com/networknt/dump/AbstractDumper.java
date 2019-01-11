package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * an abstract class which contains common properties of each dumper
 */
abstract class AbstractDumper{
    protected final HttpServerExchange exchange;
    protected final DumpConfig config;

    AbstractDumper(DumpConfig config, HttpServerExchange exchange) {
        this.config = config;
        this.exchange = exchange;
    }

    /**
     * each dumper should finally put http info to a result passed in. should be called when dump request/response
     * @param result a Map<String, Object> you want to put dumping info to.
     */
    abstract protected void putDumpInfoTo(Map<String, Object> result);
}
