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

package com.networknt.server;

/**
 * If you want close database connections, release the resource allocated
 * in the application before server shutdown, please implement this interface
 * with a class and put it into your API project
 * /src/main/resource/config/service.yml com.networknt.server.ShutdownHookProvider
 *
 * All shutdown hooks will be called during server shutdown so that resource can
 * be released completely.
 *
 * @author Steve Hu
 */
public interface ShutdownHookProvider {
    /**
     * Every implementation must implement this onShutdown method to hook in
     * some business logic during server shutdown phase.
     */
    void onShutdown();
}
