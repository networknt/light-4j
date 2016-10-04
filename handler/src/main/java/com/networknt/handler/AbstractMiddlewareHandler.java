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

package com.networknt.handler;
import io.undertow.server.HttpHandler;

/**
 * Created by steve on 03/10/16.
 */
public abstract class AbstractMiddlewareHandler implements HttpHandler {
    protected volatile HttpHandler next;

    public abstract int getPriority();
    public abstract String getHandlerType();

    public boolean isDefaultImpl() {
        return false;
    };

    public AbstractMiddlewareHandler(final HttpHandler next) {
        this.next = next;
    };

}
