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
 * A user handler provider interface. The framework has some middleware handlers and
 * these are wired into the request/response chain at the right sequence. At the end
 * of the request chain, the user business logic need to be called to do the real
 * processing and it is usually implemented as a serial of handlers. These handlers
 * needs to be grouped together and mapped to certain URLs and http methods.
 *
 * The mapping class implements this HandlerProvider so that it can be loaded during
 * server startup to inject into the handler chain.
 *
 * @author Steve Hu
 */
public interface HandlerProvider {
    /**
     * Every handler provider needs to implement this method to return a HttpHanlder or
     * a chain of HttpHandlers.
     *
     * @return HttpHandler
     */
    HttpHandler getHandler();
}
