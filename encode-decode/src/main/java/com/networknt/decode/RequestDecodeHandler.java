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

package com.networknt.decode;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.conduits.GzipStreamSourceConduit;
import io.undertow.conduits.InflatingStreamSourceConduit;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.CopyOnWriteMap;
import io.undertow.util.Headers;
import org.xnio.conduits.StreamSourceConduit;

import java.util.List;
import java.util.Map;

/**
 * This middleware handler is responsible for decode gzip request body in the request chain. It is used
 * to handle request that is gziped from the client. If you have request in both json and gzip content,
 * you can safely wire in this handler in the chain as it is only called when the content is encoded in
 * gzip.
 *
 * @author Steve Hu
 */
public class RequestDecodeHandler implements MiddlewareHandler {

    public static RequestDecodeConfig config =
            (RequestDecodeConfig)Config.getInstance().getJsonObjectConfig(RequestDecodeConfig.CONFIG_NAME, RequestDecodeConfig.class);

    private final Map<String, ConduitWrapper<StreamSourceConduit>> requestEncodings = new CopyOnWriteMap<>();

    private volatile HttpHandler next;

    public RequestDecodeHandler() {
        List<String> decoders = config.getDecoders();
        for(int i = 0; i < decoders.size(); i++) {
            String decoder = decoders.get(i);
            if(Constants.ENCODE_DEFLATE.equals(decoder)) {
                requestEncodings.put(decoder, InflatingStreamSourceConduit.WRAPPER);
            } else if(Constants.ENCODE_GZIP.equals(decoder)) {
                requestEncodings.put(decoder, GzipStreamSourceConduit.WRAPPER);
            } else {
                throw new RuntimeException("Invalid decoder " + decoder + " for RequestDecodeHandler.");
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
        ModuleRegistry.registerModule(RequestDecodeHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(RequestDecodeConfig.CONFIG_NAME), null);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        ConduitWrapper<StreamSourceConduit> encodings = requestEncodings.get(exchange.getRequestHeaders().getFirst(Headers.CONTENT_ENCODING));
        if (encodings != null && exchange.isRequestChannelAvailable()) {
            exchange.addRequestWrapper(encodings);
            // Nested handlers or even servlet filters may implement logic to decode encoded request data.
            // Since the data is no longer encoded, we remove the encoding header.
            exchange.getRequestHeaders().remove(Headers.CONTENT_ENCODING);
        }
        Handler.next(exchange, next);
    }
}
