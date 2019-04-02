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

package com.networknt.encode;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.AllowedContentEncodings;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;

import java.util.List;

/**
 * This is a middleware handler that you can wire in to the response chain to gzip large content
 * body in order to speed up the delivery and reduce the bandwidth usage.
 *
 * @author Steve Hu
 */
public class ResponseEncodeHandler implements MiddlewareHandler {
    public static ResponseEncodeConfig config =
        (ResponseEncodeConfig)Config.getInstance().getJsonObjectConfig(ResponseEncodeConfig.CONFIG_NAME, ResponseEncodeConfig.class);

    static final String NO_ENCODING_HANDLER = "ERR10050";
    private final ContentEncodingRepository contentEncodingRepository;

    private volatile HttpHandler next;


    public ResponseEncodeHandler() {
        contentEncodingRepository = new ContentEncodingRepository();
        List<String> encoders = config.getEncoders();
        for(int i = 0; i < encoders.size(); i++) {
            String encoder = encoders.get(i);
            if(Constants.ENCODE_GZIP.equals(encoder)) {
                contentEncodingRepository.addEncodingHandler(encoder, new GzipEncodingProvider(), 100);
            } else if(Constants.ENCODE_DEFLATE.equals(encoder)) {
                contentEncodingRepository.addEncodingHandler(encoder, new DeflateEncodingProvider(), 10);
            } else {
                throw new RuntimeException("Invalid encoder " + encoder + " for ResponseEncodeHandler.");
            }
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
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
        ModuleRegistry.registerModule(ResponseEncodeHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(ResponseEncodeConfig.CONFIG_NAME), null);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        AllowedContentEncodings encodings = contentEncodingRepository.getContentEncodings(exchange);
        if (encodings == null || !exchange.isResponseChannelAvailable()) {
            Handler.next(exchange, next);
        } else if (encodings.isNoEncodingsAllowed()) {
            setExchangeStatus(exchange, NO_ENCODING_HANDLER);
            return;
        } else {
            exchange.addResponseWrapper(encodings);
            exchange.putAttachment(AllowedContentEncodings.ATTACHMENT_KEY, encodings);
            Handler.next(exchange, next);
        }
    }
}
