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

import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is the entry class to execute dump feature
 * it use DumperFactory to create different dumpers and let each dumper dump http info to a Map<String, Object> result
 */
class RootDumper {
    private DumperFactory dumperFactory;
    private DumpConfig dumpConfig;
    private HttpServerExchange exchange;

    public RootDumper(DumpConfig dumpConfig, HttpServerExchange exchange) {
        this.dumpConfig = dumpConfig;
        this.exchange = exchange;
        dumperFactory = new DumperFactory();
    }

    /**
     * create dumpers that can dump http request info, and put http request info into Map<String, Object> result
     * @param result a Map<String, Object> to put http request info to
     */
    public void dumpRequest(Map<String, Object> result) {
        if(!dumpConfig.isRequestEnabled()) { return; }

        Map<String, Object> requestResult = new LinkedHashMap<>();
        for(IRequestDumpable dumper: dumperFactory.createRequestDumpers(dumpConfig, exchange)) {
            if(dumper.isApplicableForRequest()){
                dumper.dumpRequest(requestResult);
            }
        }
        result.put(DumpConstants.REQUEST, requestResult);
    }

    /**
     * create dumpers that can dump http response info, and put http response info into Map<String, Object> result
     * @param result a Map<String, Object> to put http response info to
     */
    public void dumpResponse(Map<String, Object> result) {
        if(!dumpConfig.isResponseEnabled()) { return; }

        Map<String, Object> responseResult = new LinkedHashMap<>();
        for(IResponseDumpable dumper: dumperFactory.createResponseDumpers(dumpConfig, exchange)) {
            if (dumper.isApplicableForResponse()) {
                dumper.dumpResponse(responseResult);
            }
        }
        result.put(DumpConstants.RESPONSE, responseResult);
    }
}
