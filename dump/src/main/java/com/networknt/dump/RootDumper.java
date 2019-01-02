package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

class RootDumper {
    private DumperFactory dumperFactory;
    private DumpConfig dumpConfig;
    private HttpServerExchange exchange;

    public RootDumper(DumpConfig dumpConfig, HttpServerExchange exchange) {
        this.dumpConfig = dumpConfig;
        this.exchange = exchange;
        dumperFactory = new DumperFactory();
    }

    public void dumpRequest(Map<String, Object> result) {
        if(!dumpConfig.isRequestEnabled()) { return; }

        Map<String, Object> requestResult = new LinkedHashMap<>();
        for(IRequestDumpable dumper: dumperFactory.createRequestDumpers(dumpConfig, exchange)) {
            dumper.dumpRequest(requestResult);
        }
        result.put(DumpConstants.REQUEST, requestResult);
    }

    public void dumpResponse(Map<String, Object> result) {
        if(!dumpConfig.isResponseEnabled()) { return; }

        Map<String, Object> responseResult = new LinkedHashMap<>();
        for(IResponseDumpable dumper: dumperFactory.createResponseDumpers(dumpConfig, exchange)) {
            dumper.dumpResponse(responseResult);
        }
        result.put(DumpConstants.RESPONSE, responseResult);
    }

}
