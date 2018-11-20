package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.List;
import java.util.Map;

interface IDumpable {
    default void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Boolean configObject){}

    default void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Map configObject){}

    default void dumpOption(Map<String, Object> result, HttpServerExchange exchange, List<?> configObject){}
}
