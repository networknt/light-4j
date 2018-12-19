package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

public class RootDumper {
    private DumperFactory dumperFactory;
    private Object dumpConfig;
    private HttpServerExchange exchange;

    public RootDumper(Object dumpConfig, HttpServerExchange exchange) {
        this.dumpConfig = dumpConfig;
        this.exchange = exchange;
        dumperFactory = new DumperFactory();
    }

    public void dumpRequest(Map<String, Object> result) {
        Map<String, Object> requestResult = new LinkedHashMap<>();
        Object requestConfig = ((Map)dumpConfig).get(DumpConstants.REQUEST);
        for(IRequestDumpable dumper: dumperFactory.createRequestDumpers(requestConfig, exchange)) {
            dumper.dumpRequest(requestResult);
        }
        result.put(DumpConstants.REQUEST, requestResult);
    }

    public void dumpResponse(Map<String, Object> result) {
        Map<String, Object> responseResult = new LinkedHashMap<>();
        Object responseConfig = ((Map)dumpConfig).get(DumpConstants.RESPONSE);
        for(IResponseDumpable dumper: dumperFactory.createResponseDumpers(responseConfig, exchange)) {
            dumper.dumpResponse(responseResult);
        }
        result.put(DumpConstants.RESPONSE, responseResult);
    }
}
