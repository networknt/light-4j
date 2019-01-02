package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

public class RootDumper {
    private DumperFactory dumperFactory;
    private Object dumpConfig;
    private HttpServerExchange exchange;
    private boolean enabled;
    private boolean maskEnabled;

    public RootDumper(Object dumpConfig, HttpServerExchange exchange) {
        this.dumpConfig = dumpConfig;
        this.exchange = exchange;
        dumperFactory = new DumperFactory();
        this.enabled = (Boolean)((Map)dumpConfig).get(DumpConstants.ENABLED);
        this.maskEnabled = (Boolean)((Map)dumpConfig).get(DumpConstants.MASK);
    }

    public void dumpRequest(Map<String, Object> result) {
        if(!enabled) { return; }

        Map<String, Object> requestResult = new LinkedHashMap<>();
        Object requestConfig = ((Map)dumpConfig).get(DumpConstants.REQUEST);
        for(IRequestDumpable dumper: dumperFactory.createRequestDumpers(requestConfig, exchange, maskEnabled)) {
            dumper.dumpRequest(requestResult);
        }
        result.put(DumpConstants.REQUEST, requestResult);
    }

    public void dumpResponse(Map<String, Object> result) {
        if(!enabled) { return; }

        Map<String, Object> responseResult = new LinkedHashMap<>();
        Object responseConfig = ((Map)dumpConfig).get(DumpConstants.RESPONSE);
        for(IResponseDumpable dumper: dumperFactory.createResponseDumpers(responseConfig, exchange, maskEnabled)) {
            dumper.dumpResponse(responseResult);
        }
        result.put(DumpConstants.RESPONSE, responseResult);
    }

}
