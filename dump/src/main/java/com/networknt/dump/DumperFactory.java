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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.networknt.dump.DumpConstants.*;

/**
 * a factory class to create IRequestDumpable dumpers and IResponseDumpable dumpers
 */
class DumperFactory {
    private Logger logger = LoggerFactory.getLogger(DumperFactory.class);
    //list of dumpers that can dump http request info
    private List<String> requestDumpers = Arrays.asList(BODY, COOKIES, HEADERS, QUERY_PARAMETERS, URL);
    //list of dumpers that can dump http response info
    private List<String> responseDumpers = Arrays.asList(BODY, COOKIES, HEADERS, STATUS_CODE);

    /**
     * use RequestDumperFactory to create dumpers listed in this.requestDumpers
     * @param config type: DumpConfig
     * @param exchange type: HttpServerExchange
     * @return a list of IRequestDumpable, dumpers can dump http request info
     */
    public List<IRequestDumpable> createRequestDumpers(DumpConfig config, HttpServerExchange exchange) {

        RequestDumperFactory factory = new RequestDumperFactory();
        List<IRequestDumpable> dumpers = new ArrayList<>();
        for(String dumperNames: requestDumpers) {
            IRequestDumpable dumper = factory.create(dumperNames, config, exchange);
            dumpers.add(dumper);
        }
        return dumpers;
    }

    /**
     * use ResponseDumperFactory to create dumpers listed in this.responseDumpers
     * @param config type: DumpConfig
     * @param exchange type: HttpServerExchange
     * @return a list of IResponseDumpable, dumpers can dump http response info
     */
    public List<IResponseDumpable> createResponseDumpers(DumpConfig config, HttpServerExchange exchange) {
        ResponseDumperFactory factory = new ResponseDumperFactory();
        List<IResponseDumpable> dumpers = new ArrayList<>();
        for(String dumperNames: responseDumpers) {
            IResponseDumpable dumper = factory.create(dumperNames, config, exchange);
            dumpers.add(dumper);
        }
        return dumpers;
    }

    /**
     * this class is an inner class of DumperFactory, to create IRequestDumpable dumpers which can dump http request info.
     */
    class RequestDumperFactory{
        /**
         * create IRequestDumpable dumper.
         * @param dumperName dumper name, need it to identify which dumper will be created
         * @param config type: DumpConfig, needed by dumper constructor
         * @param exchange type: HttpServerExchange, needed by dumper constructor
         * @return IRequestDumpable dumper
         */
        IRequestDumpable create(String dumperName, DumpConfig config, HttpServerExchange exchange) {
            switch (dumperName) {
                case DumpConstants.BODY:
                    return new BodyDumper(config, exchange);
                case  DumpConstants.COOKIES:
                    return new CookiesDumper(config, exchange);
                case DumpConstants.HEADERS:
                    return new HeadersDumper(config, exchange);
                case DumpConstants.QUERY_PARAMETERS:
                    return new QueryParametersDumper(config, exchange);
                case DumpConstants.URL:
                    return new UrlDumper(config, exchange);
                default:
                    logger.error("unsupported dump type: {}", dumperName);
                    return null;
            }
        }
    }

    /**
     * this class is an inner class of DumperFactory, to create IResponseDumpable dumpers which can dump http response info.
     */
    class ResponseDumperFactory{
        /**
         *
         * @param dumperName dumper name, need it to identify which dumper will be created
         * @param config type: DumpConfig, needed by dumper constructor
         * @param exchange type: HttpServerExchange, needed by dumper constructor
         * @return IRequestDumpable dumper
         */
        IResponseDumpable create(String dumperName, DumpConfig config, HttpServerExchange exchange) {
            switch (dumperName) {
                case DumpConstants.BODY:
                    return new BodyDumper(config, exchange);
                case  DumpConstants.COOKIES:
                    return new CookiesDumper(config, exchange);
                case DumpConstants.HEADERS:
                    return new HeadersDumper(config, exchange);
                case DumpConstants.STATUS_CODE:
                    return new StatusCodeDumper(config, exchange);
                default:
                    logger.error("unsupported dump type: {}", dumperName);
                    return null;
            }
        }
    }
}
