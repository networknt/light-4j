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

package com.networknt.utility;

/**
 * Constants shared by all light-4j components.
 *
 * @author Steve Hu
 */
public class Constants {
    // headers

    public static final String CORRELATION_ID_STRING = "X-Correlation-Id";
    public static final String TRACEABILITY_ID_STRING = "X-Traceability-Id";
    public static final String USER_ID_STRING = "user_id";
    public static final String CLIENT_ID_STRING = "client_id";
    public static final String SCOPE_CLIENT_ID_STRING = "scope_client_id";
    public static final String SCOPE_STRING = "scope";
    public static final String ENDPOINT_STRING = "endpoint";
    public static final String CSRF_STRING = "csrf";

    // Swagger 2.0 operation header name
    public static final String SWAGGER_OPERATION_STRING = "swagger_operation";
    // OpenAPI 3.0 operation header name
    public static final String OPENAPI_OPERATION_STRING = "openapi_operation";

    public static final String SCOPE_TOKEN_STRING = "X-Scope-Token";
    public static final String CONSUL_TOKEN_STRING = "X-Consul-Token";

    public static final String CSRF_TOKEN_STRING = "X-CSRF-TOKEN";

    // Logger
    public static final String AUDIT_LOGGER = "Audit";

    // JWT claims for fine-grained authorization in business context.
    // The key for the subject token claims in auditInfo after security handler
    // This token is passed in from Authorization header
    public static final String SUBJECT_CLAIMS = "subject_claims";
    // The key for the access token claims in auditInfo after security handler
    // This token is passed in from X-Scope-Token and it is optional
    public static final String ACCESS_CLAIMS = "access_claims";


    // Framework
    public static final String FRAMEWORK_NAME = "light";
    public static final String METHOD_CONFIG_PREFIX = "methodconfig.";

    // Switcher
    public static final String REGISTRY_HEARTBEAT_SWITCHER = "RegistryHeartBeat";

    public static final int MILLS = 1;
    public static final int SECOND_MILLS = 1000;
    public static final int MINUTE_MILLS = 60 * SECOND_MILLS;

    public static final String DEFAULT_VERSION = "1.0";
    public static final String DEFAULT_VALUE = "default";
    public static final int DEFAULT_INT_VALUE = 0;
    public static final String DEFAULT_CHARACTER = "utf-8";

    public static final String NODE_TYPE_SERVICE = "service";

    public static final String PROTOCOL_SEPARATOR = "://";
    public static final String PROTOCOL_LIGHT = "light";
    public static final String PROTOCOL_HTTPS = "https";
    public static final String TAG_ENVIRONMENT = "environment";
    public static final String PATH_SEPARATOR = "/";

    public static final String REGISTRY_PROTOCOL_LOCAL = "local";
    public static final String REGISTRY_PROTOCOL_DIRECT = "direct";
    public static final String REGISTRY_PROTOCOL_ZOOKEEPER = "zookeeper";

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/light";
    public static final String ZOOKEEPER_REGISTRY_COMMAND = "/command";

    // Headers for light-router
    public static final String SERVICE_ID_STRING = "service_id";
    public static final String ENV_TAG_STRING = "env_tag";
    public static final String HASH_KEY_STRING = "hash_key";
    public static final String HTTPS = "https";
}
