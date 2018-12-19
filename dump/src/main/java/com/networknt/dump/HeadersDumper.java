package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeadersDumper extends AbstractFilterableDumper implements IRequestDumpable, IResponseDumpable {
    Map<String, Object> headerMap = new LinkedHashMap<>();
    public HeadersDumper(Object parentConfig, HttpServerExchange exchange) {
        super(parentConfig, exchange);
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.HEADERS);
        loadFilterConfig(DumpConstants.FILTERED_HEADERS);
    }

    @Override
    public void putDumpInfoTo(Map<String, Object> result) {
        if(this.headerMap.size() > 0) {
            result.put(DumpConstants.HEADERS, this.headerMap);
        }
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        HeaderMap headers = exchange.getRequestHeaders();
        dumpHeaders(headers);
        this.putDumpInfoTo(result);
    }

    @Override
    public void dumpResponse(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        HeaderMap headers = exchange.getResponseHeaders();
        dumpHeaders(headers);
        this.putDumpInfoTo(result);
    }

    /**
     * put headers info to headerMap
     * @param headers typs: HeaderMap, get from response or request
     */
    private void dumpHeaders(HeaderMap headers) {
        headers.forEach((headerValues) -> headerValues.forEach((headerValue) -> {
            String headerName = headerValues.getHeaderName().toString();
            if(!this.filter.contains(headerName)) {
                headerMap.put(headerName, headerValue);
            }
        }));
    }
}
