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

package com.networknt.jaeger.tracing;

import com.networknt.audit.AuditHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.networknt.httpstring.AttachmentConstants.ROOT_SPAN;
import static com.networknt.httpstring.AttachmentConstants.EXCHANGE_TRACER;
import static com.networknt.jaeger.tracing.JaegerStartupHookProvider.tracer;

/**
 * OpenTracing Jaeger Handler
 *
 * @author Steve Hu
 */
public class JaegerHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(JaegerHandler.class);

    static JaegerConfig jaegerConfig = (JaegerConfig) Config.getInstance().getJsonObjectConfig(JaegerConfig.CONFIG_NAME, JaegerConfig.class);


    private volatile HttpHandler next;

    public JaegerHandler() {

    }

    /**
     * Extract the context, start and stop the span here.
     *
     * @param exchange HttpServerExchange
     * @throws Exception Exception
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // get the path and method to construct the endpoint for the operation of tracing.
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        String endpoint = null;
        if(auditInfo != null) {
            endpoint = (String)auditInfo.get(Constants.ENDPOINT_STRING);
        } else {
            endpoint = exchange.getRequestPath() + "@" + exchange.getRequestMethod();
        }

        HeaderMap headerMap = exchange.getRequestHeaders();
        final HashMap<String, String> headers = new HashMap<>();
        for(HttpString key : headerMap.getHeaderNames()) {
            headers.put(key.toString(), headerMap.getFirst(key));
        }
        TextMap carrier = new TextMapAdapter(headers);

        // start the server span.
        Tracer.SpanBuilder spanBuilder;
        try {
            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, carrier);
            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan(endpoint);
            } else {
                spanBuilder = tracer.buildSpan(endpoint).asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan(endpoint);
        }
        Span rootSpan = spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).start();
        tracer.activateSpan(rootSpan);
        // This can be retrieved in the business handler to add tags and logs for tracing.
        exchange.putAttachment(ROOT_SPAN, rootSpan);
        // The client module can use this to inject tracer.
        exchange.putAttachment(EXCHANGE_TRACER, tracer);

        // add an exchange complete listener to close the Root Span for the request.
        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            Span span = exchange1.getAttachment(ROOT_SPAN);
            if(span != null) {
                span.finish();
            }
            nextListener.proceed();
        });

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
        return jaegerConfig.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(JaegerHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(JaegerConfig.CONFIG_NAME), null);
    }

}
