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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * an abstract class which contains common properties of each dumper
 */
abstract class AbstractDumper{
    protected final HttpServerExchange exchange;
    protected final DumpConfig config;

    AbstractDumper(DumpConfig config, HttpServerExchange exchange) {
        this.config = config;
        this.exchange = exchange;
    }

    /**
     * each dumper should finally put http info to a result passed in. should be called when dump request/response
     * @param result a Map you want to put dumping info to.
     */
    abstract protected void putDumpInfoTo(Map<String, Object> result);
}
