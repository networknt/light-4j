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

package com.networknt.sanitizer;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.owasp.encoder.EncoderWrapper;
import org.owasp.encoder.Encoders;

import java.util.*;

/**
 * This is a middleware component that sanitize cross site scripting tags in request. As potentially
 * sanitizing body of the request, this middleware must be plugged into the chain after body parser.
 *
 * Note: the sanitizer only works with JSON body, for other types, it will be skipped.
 *
 * @author Steve Hu
 */
public class SanitizerHandler implements MiddlewareHandler {

    static SanitizerConfig config;

    EncoderWrapper bodyEncoder;
    EncoderWrapper headerEncoder;
    private volatile HttpHandler next;

    public SanitizerHandler() {
        config = SanitizerConfig.load();
        bodyEncoder = new EncoderWrapper(Encoders.forName(config.getBodyEncoder()), config.getBodyAttributesToIgnore(), config.getBodyAttributesToEncode());
        headerEncoder = new EncoderWrapper(Encoders.forName(config.getHeaderEncoder()), config.getHeaderAttributesToIgnore(), config.getHeaderAttributesToEncode());
    }

    // integration test purpose only.
    @Deprecated
    public SanitizerHandler(String configName) {
        config = SanitizerConfig.load(configName);
        bodyEncoder = new EncoderWrapper(Encoders.forName(config.getBodyEncoder()), config.getBodyAttributesToIgnore(), config.getBodyAttributesToEncode());
        headerEncoder = new EncoderWrapper(Encoders.forName(config.getHeaderEncoder()), config.getHeaderAttributesToIgnore(), config.getHeaderAttributesToEncode());
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.trace("SanitizerHandler.handleRequest starts.");
        String method = exchange.getRequestMethod().toString();
        if (config.isHeaderEnabled()) {
            HeaderMap headerMap = exchange.getRequestHeaders();
            if (headerMap != null) {
                for (HeaderValues values : headerMap) {
                    if (values != null) {
                        // if ignore list exists, it will take the precedence.
                        if(config.getHeaderAttributesToIgnore() != null && config.getHeaderAttributesToIgnore().stream().anyMatch(values.getHeaderName().toString()::equalsIgnoreCase)) {
                            if(logger.isTraceEnabled()) logger.trace("Ignore header " + values.getHeaderName().toString() + " as it is in the ignore list.");
                            continue;
                        }

                        if(config.getHeaderAttributesToEncode() != null) {
                            if(config.getHeaderAttributesToEncode().stream().anyMatch(values.getHeaderName().toString()::equalsIgnoreCase)) {
                                if(logger.isTraceEnabled()) logger.trace("Encode header " + values.getHeaderName().toString() + " as it is not in the ignore list and it is in the encode list.");
                                ListIterator<String> itValues = values.listIterator();
                                while (itValues.hasNext()) {
                                    itValues.set(headerEncoder.applyEncoding(itValues.next()));
                                }
                            }
                        } else {
                            // no attributes to encode, encode everything except the ignore list.
                            if(logger.isTraceEnabled()) logger.trace("Encode header " + values.getHeaderName().toString() + " as it is not in the ignore list and the encode list is null.");
                            ListIterator<String> itValues = values.listIterator();
                            while (itValues.hasNext()) {
                                itValues.set(headerEncoder.applyEncoding(itValues.next()));
                            }
                        }
                    }
                }
            }
        }

        if (config.isBodyEnabled() && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
            // assume that body parser is installed before this middleware and body is parsed as a map.
            // we are talking about JSON api now.
            Object body = exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
            if (body != null) {
                if(body instanceof List) {
                    bodyEncoder.encodeList((List<Map<String, Object>>)body);
                } else if (body instanceof Map){
                    // assume it is a map here.
                    bodyEncoder.encodeNode((Map<String, Object>)body);
                } else {
                    // Body is not in JSON format or form data, skip...
                }
            }
        }
        if (logger.isDebugEnabled()) logger.trace("SanitizerHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SanitizerConfig.CONFIG_NAME, SanitizerHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(SanitizerConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config = SanitizerConfig.load();
        ModuleRegistry.registerModule(SanitizerConfig.CONFIG_NAME, SanitizerHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(SanitizerConfig.CONFIG_NAME), null);
    }

}
