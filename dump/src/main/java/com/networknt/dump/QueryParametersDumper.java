package com.networknt.dump;

import com.networknt.mask.Mask;
import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryParametersDumper extends AbstractFilterableDumper implements IRequestDumpable {
    private Map<String, Object> queryParametersMap = new LinkedHashMap<>();

    public QueryParametersDumper(Object parentConfig, HttpServerExchange exchange, Boolean maskEnabled) {
        super(parentConfig, exchange, maskEnabled);
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.QUERY_PARAMETERS);
        loadFilterConfig(DumpConstants.FILTERED_QUERY_PARAMETERS);
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        exchange.getQueryParameters().forEach((k, v) -> {
            if (!this.filter.contains(k)) {
                //mask query parameter value
                String queryParameterValue = isMaskEnabled() ? Mask.maskRegex( v.getFirst(), "queryParameter", k) : v.getFirst();
                queryParametersMap.put(k, queryParameterValue);
            }
        });
        this.putDumpInfoTo(result);
    }

    @Override
    public void putDumpInfoTo(Map<String, Object> result) {
        if(this.queryParametersMap.size() > 0) {
            result.put(DumpConstants.QUERY_PARAMETERS, queryParametersMap);
        }
    }
}
