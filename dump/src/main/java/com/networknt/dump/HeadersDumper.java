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
import io.undertow.util.HeaderMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Header Dumper is to dump http request/response header info to result.
 */
public class HeadersDumper extends AbstractDumper implements IRequestDumpable, IResponseDumpable {
    private Map<String, Object> headerMap = new LinkedHashMap<>();
    public HeadersDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * put headerMap to result.
     * @param result a Map you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(this.headerMap.size() > 0) {
            result.put(DumpConstants.HEADERS, this.headerMap);
        }
    }

    /**
     * impl of dumping request headers to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpRequest(Map<String, Object> result) {
        HeaderMap headers = exchange.getRequestHeaders();
        dumpHeaders(headers);
        if(config.isMaskEnabled()) {
            this.headerMap.forEach((s, o) -> headerMap.put(s, Mask.maskRegex((String) o, "requestHeader", s)));
        }
        this.putDumpInfoTo(result);
    }

    /**
     * impl of dumping response headers to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpResponse(Map<String, Object> result) {
        HeaderMap headers = exchange.getResponseHeaders();
        dumpHeaders(headers);
        if(config.isMaskEnabled()) {
            this.headerMap.forEach((s, o) -> headerMap.put(s, Mask.maskRegex((String) o, "responseHeader", s)));
        }
        this.putDumpInfoTo(result);
    }

    /**
     * put headers info to headerMap
     * @param headers types: HeaderMap, get from response or request
     */
    private void dumpHeaders(HeaderMap headers) {
        headers.forEach((headerValues) -> headerValues.forEach((headerValue) -> {
            String headerName = headerValues.getHeaderName().toString();
            if(!config.getRequestFilteredHeaders().contains(headerName)) {
                headerMap.put(headerName, headerValue);
            }
        }));
    }

    @Override
    public boolean isApplicableForRequest() {
        return config.isRequestHeaderEnabled();
    }

    @Override
    public boolean isApplicableForResponse() {
        return config.isResponseHeaderEnabled();
    }
}
