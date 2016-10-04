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

package com.networknt.security;

import com.networknt.handler.AbstractMiddlewareHandler;
import io.undertow.server.HttpHandler;

/**
 * Created by steve on 03/10/16.
 */
public abstract class AbstractSecurityHandler extends AbstractMiddlewareHandler {

    final static int priority = 950;
    final static String handlerType = "security";

    public int getPriority() {
        return priority;
    }

    public String getHandlerType() {
        return handlerType;
    }

    public AbstractSecurityHandler(HttpHandler next) {
        super(next);
    }
}
