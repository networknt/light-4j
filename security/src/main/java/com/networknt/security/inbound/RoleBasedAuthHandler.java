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

package com.networknt.security.inbound;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Role-Based Authorization Handler is an abstract handler to define the structure of the fine-grained
 * Authorization within business context. We don't provide the final implementation as we don't have
 * the business knowledge to do so.
 *
 * Assume role will be passed in from JwT token as role with a list of role ids like scopes.
 *
 * @author Steve Hu
 */
public abstract class RoleBasedAuthHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

    }


}
