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
    public static final String ADM_PASSTHROUGH_STRING = "X-Adm-PassThrough";

    public static final String USER_ID_STRING = "user_id";
    public static final String UID = "uid";
    public static final String USER_TYPE = "user_type";
    public static final String ROLES = "roles";
    public static final String CLIENT_ID_STRING = "client_id";
    public static final String CID = "cid";
    public static final String ISS = "iss";
    public static final String CALLER_ID_STRING = "caller_id";
    public static final String SCOPE_CLIENT_ID_STRING = "scope_client_id";
    public static final String SCOPE_STRING = "scope";
    public static final String SCP_STRING = "scp";
    // use for light-aws-lambda to pass the scopes to the scope verifier from authorizer
    public static final String PRIMARY_SCOPES = "primary_scopes";
    public static final String SECONDARY_SCOPES = "secondary_scopes";
    public static final String ENDPOINT_STRING = "endpoint";
    public static final String UNKNOWN = "unknown";
    public static final String CSRF = "csrf";
    public static final String AUTHORIZATION_STRING = "authorization";
    public static final String EID = "eid";
    public static final String ATT = "att";
    public static final String POS = "pos";
    public static final String GRP = "grp";
    public static final String ROLE = "role";
    public static final String HOST = "host";
    public static final String EML = "eml";
    public static final String EMAIL = "email";
    public static final String GROUPS = "groups";
    public static final String GROUP = "group";
    public static final String POSITIONS = "positions";
    public static final String POSITION = "position";
    public static final String ATTRIBUTES = "attributes";
    public static final String ATTRIBUTE = "attribute";
    public static final String USERS = "users";
    public static final String USER = "user";
    public static final String COL = "col";
    public static final String ROW = "row";

    // Swagger 2.0 operation header name
    public static final String SWAGGER_OPERATION_STRING = "swagger_operation";
    // OpenAPI 3.0 operation header name
    public static final String OPENAPI_OPERATION_STRING = "openapi_operation";
    // Hybrid service schema
    public static final String HYBRID_SERVICE_ID = "hybrid_service_id";
    public static final String HYBRID_SERVICE_MAP = "hybrid_service_map";
    public static final String HYBRID_SERVICE_DATA = "hybrid_service_data";

    public static final String SCOPE_TOKEN_STRING = "X-Scope-Token";
    public static final String CONSUL_TOKEN_STRING = "X-Consul-Token";

    public static final String CSRF_TOKEN_STRING = "X-CSRF-TOKEN";

    // Logging and Auditing
    public static final String AUDIT_LOGGER = "Audit";
    public static final String STATUS = "Status";
    public static final String STACK_TRACE = "StackTrace";
    public static final String JSONRPC_ID = "jsonrpc_id";
    // JWT claims for fine-grained authorization in business context.
    public static final String AUDIT_INFO = "auditInfo";
    // The key for the subject token claims in auditInfo after security handler
    // This token is passed in from Authorization header
    public static final String SUBJECT_CLAIMS = "subject_claims";
    // The key for the access token claims in auditInfo after security handler
    // This token is passed in from X-Scope-Token and it is optional
    public static final String ACCESS_CLAIMS = "access_claims";
    // The key for the issuer token claims in auditInfo
    public static final String ISSUER_CLAIMS = "issuer_claims";

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
    public static final String SERVICE_URL_STRING = "service_url";
    public static final String ENV_TAG_STRING = "env_tag";
    public static final String HASH_KEY_STRING = "hash_key";
    public static final String HTTPS = "https";
    public static final String HEADER = "header";
    public static final String PROTOCOL = "protocol";

    // Encode and decode for gzip and deflate
    public static final String ENCODE_GZIP = "gzip";
    public static final String ENCODE_DEFLATE = "deflate";

    //Rate Limit
    public static final String RATELIMIT_LIMIT = "RateLimit-Limit";
    public static final String RATELIMIT_REMAINING = "RateLimit-Remaining";
    public static final String RATELIMIT_RESET = "RateLimit-Reset";
    public static final String RETRY_AFTER = "Retry-After";

    // rule loader
    public static final String RULE_ID = "ruleId";

    // framework
    public static final String LIGHT_4J = "Light4j";
    public static final String SPRING_BOOT = "SpringBoot";

    // plugin error message
    public static final String ERROR_MESSAGE = "errorMessage";

    // token exchange type
    public static final String TOKEN_EX_TYPE_CCAC = "ccac"; // client credentials to authorization code.
    public static final String TOKEN_EX_TYPE_MSAL = "msal"; // Microsoft authentication library
}
