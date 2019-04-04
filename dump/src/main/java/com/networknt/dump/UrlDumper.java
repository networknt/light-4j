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
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * UrlDumper is to dump url info to result.
 */
public class UrlDumper extends AbstractDumper implements IRequestDumpable{
    private String url = "";

    public UrlDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        this.url = exchange.getRequestURL();
        if(config.isMaskEnabled()) {
            Mask.maskString(url, "uri");
        }
        this.putDumpInfoTo(result);
    }

    /**
     * put this.url to result
     * @param result a Map you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.url)) {
            result.put(DumpConstants.URL, this.url);
        }
    }

    @Override
    public boolean isApplicableForRequest() {
        return config.isRequestUrlEnabled();
    }
}
