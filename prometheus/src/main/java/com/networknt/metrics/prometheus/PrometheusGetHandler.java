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

package com.networknt.metrics.prometheus;

import com.networknt.handler.LightHttpHandler;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * The handler should be available by project code generation.
 * The default path for the handler should be /v1/prometheus
 * @author Gavin Chen
 */
public class PrometheusGetHandler implements LightHttpHandler {


    static final Logger logger = LoggerFactory.getLogger(PrometheusGetHandler.class);
    static CollectorRegistry registry =  CollectorRegistry.defaultRegistry;

    public PrometheusGetHandler(){}

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        Writer writer = new StringWriter();
        try {
            TextFormat.write004(writer, registry.metricFamilySamples());
        } catch (IOException e) {
            logger.error("error on put result:", e);
        }
        exchange.getResponseSender().send(writer.toString());

    }
}
