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

package com.networknt.httpstring;

import com.networknt.utility.Constants;
import io.undertow.util.HttpString;

/**
 * HttpString Constants shared by all light-4j components. This is moved from utility
 * module as we don't want it to be depended on Undertow.
 *
 * @author Steve Hu
 */
public class HttpStringConstants {
    // headers
    public static final HttpString CORRELATION_ID = new HttpString(Constants.CORRELATION_ID_STRING);
    public static final HttpString TRACEABILITY_ID = new HttpString(Constants.TRACEABILITY_ID_STRING);
    public static final HttpString USER_ID = new HttpString(Constants.USER_ID_STRING);
    public static final HttpString CLIENT_ID = new HttpString(Constants.CLIENT_ID_STRING);
    public static final HttpString SCOPE_CLIENT_ID = new HttpString(Constants.SCOPE_CLIENT_ID_STRING);
    public static final HttpString SCOPE = new HttpString(Constants.SCOPE_STRING);
    public static final HttpString ENDPOINT = new HttpString(Constants.ENDPOINT_STRING);
    public static final HttpString SWAGGER_OPERATION = new HttpString(Constants.SWAGGER_OPERATION_STRING);
    public static final HttpString SCOPE_TOKEN = new HttpString(Constants.SCOPE_TOKEN_STRING);
    public static final HttpString CONSUL_TOKEN = new HttpString(Constants.CONSUL_TOKEN_STRING);
    public static final HttpString CSRF_TOKEN = new HttpString(Constants.CSRF_TOKEN_STRING);

    public static final HttpString SERVICE_ID = new HttpString(Constants.SERVICE_ID_STRING);
    public static final HttpString ENV_TAG = new HttpString(Constants.ENV_TAG_STRING);
    public static final HttpString HASH_KEY = new HttpString(Constants.HASH_KEY_STRING);
}
