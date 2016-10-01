/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.body;

import com.networknt.exception.ApiException;
import com.networknt.status.Status;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * This is a handler that parses the body into a string.
 *
 * Created by steve on 29/09/16.
 */
public class BodyHandler implements HttpHandler {
    static final Logger logger = LoggerFactory.getLogger(BodyHandler.class);

    // request body will be parse during validation and it is attached to the exchange
    public static final AttachmentKey<String> REQUEST_BODY = AttachmentKey.create(String.class);

    public static final String CONFIG_NAME = "body";

    private volatile HttpHandler next;

    public BodyHandler(final HttpHandler next) {
        this.next = next;
    }


    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // parse the body to string
        if(exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();
        InputStream is = exchange.getInputStream();
        if(is != null) {
            try {
                if(is.available() != -1) {
                    String body = new Scanner(is,"UTF-8").useDelimiter("\\A").next();
                    exchange.putAttachment(REQUEST_BODY, body);
                }
            } catch (IOException e) {
                logger.error("IOException: ", e);
            }
        }
        next.handleRequest(exchange);
    }
}
