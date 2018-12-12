package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryParametersDumper extends AbstractFilterableDumper {
    private Map<String, Object> queryParametersMap = new LinkedHashMap<>();

    public QueryParametersDumper(Object parentConfig, HttpServerExchange exchange, IDumpable.HttpMessageType type) {
        super(parentConfig, exchange, type);
    }

    @Override
    public Map<String, Object> getResult() {
        return this.queryParametersMap;
    }

    @Override
    public void putResultTo(Map<String, Object> result) {
        if(this.queryParametersMap.size() > 0) {
            result.put(DumpConstants.QUERY_PARAMETERS, queryParametersMap);
        }
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.COOKIES);
        loadFilterConfig(DumpConstants.FILTERED_QUERY_PARAMETERS);
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            exchange.getQueryParameters().forEach((k, v) -> {
                if (!this.filter.contains(k)) {
                    queryParametersMap.put(k, v.getFirst());
                }
            });

        }
    }

    @Override
    protected Boolean isApplicable() {
        if(this.type.equals(IDumpable.HttpMessageType.RESPONSE)) {
            return false;
        }
        return super.isApplicable();
    }
}
