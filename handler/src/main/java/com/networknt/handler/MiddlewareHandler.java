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

package com.networknt.handler;

import io.undertow.server.HttpHandler;

/**
 * A interface for middleware handlers. All middleware handlers must implement this interface
 * so that the handler can be plugged in to the request/response chain during server startup
 * with SPI (Service Provider Interface). The entire light-4j framework is a core server that
 * provides a plugin structure to hookup all sorts of plugins to handler different cross-cutting
 * concerns.
 *
 * The middleware handlers are designed based on chain of responsibility pattern.
 *
 * This handler extends LightHttpHandler which has a default method to handle the error status
 * response.
 *
 * @author Steve Hu
 */
public interface MiddlewareHandler extends LightHttpHandler {
    /**
     * Get the next handler in the chain
     *
     * @return HttpHandler
     */
    HttpHandler getNext();

    /**
     * Set the next handler in the chain
     *
     * @param next HttpHandler
     * @return MiddlewareHandler
     */
    MiddlewareHandler setNext(final HttpHandler next);

    /**
     * Indicate if this handler is enabled or not.
     *
     * @return boolean true if enabled
     */
    boolean isEnabled();

    /**
     * Register this handler to the handler registration.
     */
    void register();
}
