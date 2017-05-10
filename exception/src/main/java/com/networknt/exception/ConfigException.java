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

package com.networknt.exception;

/**
 * A checked exception to indicate something is wrong in the configuration
 * file. Most config files will be loaded during server startup in a static
 * block and there is no way an exception can be thrown. This is a special
 * exception that need to be monitored in logs in order to capture config
 * issue during development phase.
 *
 * @author Steve Hu
 */
public class ConfigException extends Exception {
    private static final long serialVersionUID = 1L;
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
