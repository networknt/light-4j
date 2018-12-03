package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeadersDumper extends AbstractFilterableDumper {
    Map<String, Object> headerMap = new LinkedHashMap<>();
    public HeadersDumper(Object parentConfig, HttpServerExchange exchange, IDumpable.HttpMessageType type) {
        super(parentConfig, exchange, type);
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.HEADERS);
        loadFilterConfig(DumpConstants.FILTERED_HEADERS);
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            HeaderMap headers = type.equals(IDumpable.HttpMessageType.RESPONSE) ? exchange.getResponseHeaders(): exchange.getRequestHeaders();
            headers.forEach((headerValues) -> headerValues.forEach((headerValue) -> {
                String headerName = headerValues.getHeaderName().toString();
                if(!this.filter.contains(headerName)) {
                    headerMap.put(headerName, headerValue);
                }
            }));
        }
    }

    @Override
    public Map<String, Object> getResult() {
        return this.headerMap;
    }

    @Override
    public void putResultTo(Map<String, Object> result) {
        if(this.headerMap.size() > 0) {
            result.put(DumpConstants.HEADERS, this.headerMap);
        }
    }
}
