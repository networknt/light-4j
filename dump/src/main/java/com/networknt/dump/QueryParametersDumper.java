/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.dump;

import com.networknt.mask.Mask;
import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QueryParametersDumper is to dump http request query parameters info to result.
 */
public class QueryParametersDumper extends AbstractDumper implements IRequestDumpable {
    private Map<String, Object> queryParametersMap = new LinkedHashMap<>();

    public QueryParametersDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * impl of dumping request query parameter to result
     * @param result A map you want to put dump information to
     */
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

    /**
     * put queryParametersMap to result.
     * @param result a Map you want to put dumping info to.
     */
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
