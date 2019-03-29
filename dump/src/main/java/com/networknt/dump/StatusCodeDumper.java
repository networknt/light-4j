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

import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * StatusCodeDumper is to dump http response status code info to result.
 */
public class StatusCodeDumper extends AbstractDumper implements IResponseDumpable{
    private String statusCodeResult = "";

    public StatusCodeDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * impl of dumping response status code to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpResponse(Map<String, Object> result) {
        this.statusCodeResult = String.valueOf(exchange.getStatusCode());
        this.putDumpInfoTo(result);
    }

    /**
     * put this.statusCodeResult to result
     * @param result a Map you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.statusCodeResult)) {
            result.put(DumpConstants.STATUS_CODE, this.statusCodeResult);
        }
    }

    @Override
    public boolean isApplicableForResponse() {
        return config.isResponseStatusCodeEnabled();
    }

}
