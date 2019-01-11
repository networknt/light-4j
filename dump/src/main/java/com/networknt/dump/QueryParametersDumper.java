package com.networknt.dump;

import com.networknt.mask.Mask;
import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryParametersDumper extends AbstractDumper implements IRequestDumpable {
    private Map<String, Object> queryParametersMap = new LinkedHashMap<>();

    public QueryParametersDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        exchange.getQueryParameters().forEach((k, v) -> {
            if (config.getRequestFilteredQueryParameters().contains(k)) {
                //mask query parameter value
                String queryParameterValue = config.isMaskEnabled() ? Mask.maskRegex( v.getFirst(), "queryParameter", k) : v.getFirst();
                queryParametersMap.put(k, queryParameterValue);
            }
        });
        this.putDumpInfoTo(result);
    }

    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(this.queryParametersMap.size() > 0) {
            result.put(DumpConstants.QUERY_PARAMETERS, queryParametersMap);
        }
    }

    @Override
    public boolean isApplicableForRequest() {
        return config.isRequestQueryParametersEnabled();
    }
}
