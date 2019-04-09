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

import com.networknt.body.BodyHandler;
import com.networknt.mask.Mask;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * BodyDumper is to dump http request/response body info to result.
 * Right now only support json info.
 */
 class BodyDumper extends AbstractDumper implements IRequestDumpable, IResponseDumpable{
    private static final Logger logger = LoggerFactory.getLogger(BodyDumper.class);
    private String bodyContent = "";

    BodyDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * put bodyContent to result
     * @param result a Map<String, Object> you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.bodyContent)) {
            result.put(DumpConstants.BODY, this.bodyContent);
        }
    }

    /**
     * impl of dumping request body to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpRequest(Map<String, Object> result) {
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        //only dump json info
        if (contentType != null && contentType.startsWith("application/json")) {
            //if body info already grab by body handler, get it from attachment directly
            Object requestBodyAttachment = exchange.getAttachment(BodyHandler.REQUEST_BODY);
            if(requestBodyAttachment != null) {
                dumpBodyAttachment(requestBodyAttachment);
            } else {
                //otherwise get it from input stream directly
                dumpInputStream();
            }
        } else {
            logger.info("unsupported contentType: {}", contentType);
        }
        this.putDumpInfoTo(result);
    }

    /**
     * impl of dumping response body to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpResponse(Map<String, Object> result) {
        byte[] responseBodyAttachment = exchange.getAttachment(StoreResponseStreamSinkConduit.RESPONSE);
        if(responseBodyAttachment != null) {
            this.bodyContent = config.isMaskEnabled() ? Mask.maskJson(new ByteArrayInputStream(responseBodyAttachment), "responseBody") : new String(responseBodyAttachment, UTF_8);
        }
        this.putDumpInfoTo(result);
    }

    /**
     * read from input stream, convert it to string, put into this.bodyContent
     */
     private void dumpInputStream(){
        //dump request body
        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        try {
            if(config.isMaskEnabled() && inputStream.available() != -1) {
                this.bodyContent = Mask.maskJson(inputStream, "requestBody");
            } else {
                try {
                    this.bodyContent = StringUtils.inputStreamToString(inputStream, UTF_8);
                } catch (IOException e) {
                    logger.error(e.toString());
                }
            }
        } catch (IOException e) {
            logger.error("undertow inputstream error:" + e.getMessage());
        }
    }

    /**
     * read from body attachment from Body Handler, convert it to string, put into this.bodyContent
     */
    private void dumpBodyAttachment(Object requestBodyAttachment) {
        this.bodyContent = config.isMaskEnabled() ? Mask.maskJson(requestBodyAttachment, "requestBody") : requestBodyAttachment.toString();
    }

     @Override
     public boolean isApplicableForRequest() {
         return config.isRequestBodyEnabled();
     }

     @Override
     public boolean isApplicableForResponse() {
         return config.isResponseBodyEnabled();
     }
}
