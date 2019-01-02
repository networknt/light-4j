package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

abstract class AbstractDumper{
    protected final HttpServerExchange exchange;
    protected final DumpConfig config;

    AbstractDumper(DumpConfig config, HttpServerExchange exchange) {
        this.config = config;
        this.exchange = exchange;
    }

    abstract protected void putDumpInfoTo(Map<String, Object> result);
}
