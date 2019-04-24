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

package com.networknt.config;

/**
 * A runtime exception to indicate something is wrong in the configuration
 * file. Most config files will be loaded during server startup in a static
 * block and there is no way an exception can be thrown. To use a runtime
 * exception, we can force the server startup to failed so that some action
 * can be taken by the operation team. If the config file is laze loaded in
 * the application during request handling phase, then this exception will
 * cause a 500 error and most likely be handled by the exception handler in
 * the middleware handler chain.
 *
 * This is a special exception that need to be monitored in logs in order to
 * capture config issue during development phase.
 *
 */
public class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }
}
