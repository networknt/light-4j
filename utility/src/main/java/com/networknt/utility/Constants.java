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
    private Constants() {
    }
    // headers

    /** correlation ID header name */
    public static final String CORRELATION_ID_STRING = "X-Correlation-Id";
    /** traceability ID header name */
    public static final String TRACEABILITY_ID_STRING = "X-Traceability-Id";
    /** ADM passthrough header name */
    public static final String ADM_PASSTHROUGH_STRING = "X-Adm-PassThrough";

    /** user ID header name */
    public static final String USER_ID_STRING = "user_id";
    /** uid claim string */
    public static final String UID = "uid";
    /** user type header name */
    public static final String USER_TYPE = "user_type";
    /** roles claim string */
    public static final String ROLES = "roles";
    /** client ID header name */
    public static final String CLIENT_ID_STRING = "client_id";
    /** cid claim string */
    public static final String CID = "cid";
    /** iss claim string */
    public static final String ISS = "iss";
    /** caller ID header name */
    public static final String CALLER_ID_STRING = "caller_id";
    /** scope client ID header name */
    public static final String SCOPE_CLIENT_ID_STRING = "scope_client_id";
    /** scope claim string */
    public static final String SCOPE_STRING = "scope";
    /** scp claim string */
    public static final String SCP_STRING = "scp";
    // use for light-aws-lambda to pass the scopes to the scope verifier from authorizer
    /** primary scopes claim string */
    public static final String PRIMARY_SCOPES = "primary_scopes";
    /** secondary scopes claim string */
    public static final String SECONDARY_SCOPES = "secondary_scopes";
    /** endpoint claim string */
    public static final String ENDPOINT_STRING = "endpoint";
    /** unknown constant string */
    public static final String UNKNOWN = "unknown";
    /** CSRF constant string */
    public static final String CSRF = "csrf";
    /** authorization header name */
    public static final String AUTHORIZATION_STRING = "authorization";
    /** eid claim string */
    public static final String EID = "eid";
    /** att claim string */
    public static final String ATT = "att";
    /** pos claim string */
    public static final String POS = "pos";
    /** grp claim string */
    public static final String GRP = "grp";
    /** role claim string */
    public static final String ROLE = "role";
    /** host header name */
    public static final String HOST = "host";
    /** eml claim string */
    public static final String EML = "eml";
    /** email claim string */
    public static final String EMAIL = "email";
    /** groups claim string */
    public static final String GROUPS = "groups";
    /** group constant string */
    public static final String GROUP = "group";
    /** positions claim string */
    public static final String POSITIONS = "positions";
    /** position claim string */
    public static final String POSITION = "position";
    /** attributes claim string */
    public static final String ATTRIBUTES = "attributes";
    /** attribute claim string */
    public static final String ATTRIBUTE = "attribute";
    /** users claim string */
    public static final String USERS = "users";
    /** user constant string */
    public static final String USER = "user";
    /** col constant string */
    public static final String COL = "col";
    /** row constant string */
    public static final String ROW = "row";

    // Swagger 2.0 operation header name
    /** Swagger operation header name */
    public static final String SWAGGER_OPERATION_STRING = "swagger_operation";
    // OpenAPI 3.0 operation header name
    /** OpenAPI operation string */
    public static final String OPENAPI_OPERATION_STRING = "openapi_operation";
    // Hybrid service schema
    /** hybrid service ID constant */
    public static final String HYBRID_SERVICE_ID = "hybrid_service_id";
    /** hybrid service map constant */
    public static final String HYBRID_SERVICE_MAP = "hybrid_service_map";
    /** hybrid service data constant */
    public static final String HYBRID_SERVICE_DATA = "hybrid_service_data";

    /** scope token header name */
    public static final String SCOPE_TOKEN_STRING = "X-Scope-Token";
    /** consul token header name */
    public static final String CONSUL_TOKEN_STRING = "X-Consul-Token";

    /** CSRF token header name */
    public static final String CSRF_TOKEN_STRING = "X-CSRF-TOKEN";

    /** audit logger name */
    public static final String AUDIT_LOGGER = "Audit";
    /** status constant string */
    public static final String STATUS = "Status";
    /** stack trace constant string */
    public static final String STACK_TRACE = "StackTrace";
    /** JSON-RPC ID constant string */
    public static final String JSONRPC_ID = "jsonrpc_id";
    // JWT claims for fine-grained authorization in business context.
    /** audit info constant string */
    public static final String AUDIT_INFO = "auditInfo";
    // The key for the subject token claims in auditInfo after security handler
    // This token is passed in from Authorization header
    /** subject claims constant string */
    public static final String SUBJECT_CLAIMS = "subject_claims";
    // The key for the access token claims in auditInfo after security handler
    // This token is passed in from X-Scope-Token and it is optional
    /** access claims constant string */
    public static final String ACCESS_CLAIMS = "access_claims";
    /** issuer claims constant string */
    public static final String ISSUER_CLAIMS = "issuer_claims";

    // Framework
    /** framework name constant string */
    public static final String FRAMEWORK_NAME = "light";
    /** method config prefix constant string */
    public static final String METHOD_CONFIG_PREFIX = "methodconfig.";

    // Switcher
    /** registry heartbeat switcher constant string */
    public static final String REGISTRY_HEARTBEAT_SWITCHER = "RegistryHeartBeat";

    /** mills constant */
    public static final int MILLS = 1;
    /** number of milliseconds in a second */
    public static final int SECOND_MILLS = 1000;
    /** number of milliseconds in a minute */
    public static final int MINUTE_MILLS = 60 * SECOND_MILLS;
    /** default version string */
    public static final String DEFAULT_VERSION = "1.0";
    /** default value string */
    public static final String DEFAULT_VALUE = "default";
    /** default integer value */
    public static final int DEFAULT_INT_VALUE = 0;
    /** default character encoding */
    public static final String DEFAULT_CHARACTER = "utf-8";

    /** node type service constant string */
    public static final String NODE_TYPE_SERVICE = "service";

    /** protocol separator constant string */
    public static final String PROTOCOL_SEPARATOR = "://";
    /** light protocol constant string */
    public static final String PROTOCOL_LIGHT = "light";
    /** https protocol constant string */
    public static final String PROTOCOL_HTTPS = "https";
    /** tag environment constant */
    public static final String TAG_ENVIRONMENT = "environment";
    /** path separator constant string */
    public static final String PATH_SEPARATOR = "/";

    /** local registry protocol */
    public static final String REGISTRY_PROTOCOL_LOCAL = "local";
    /** direct registry protocol */
    public static final String REGISTRY_PROTOCOL_DIRECT = "direct";
    /** zookeeper registry protocol */
    public static final String REGISTRY_PROTOCOL_ZOOKEEPER = "zookeeper";

    /** zookeeper registry namespace */
    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/light";
    /** zookeeper registry command */
    public static final String ZOOKEEPER_REGISTRY_COMMAND = "/command";

    // Headers for light-router
    /** service ID string constant */
    public static final String SERVICE_ID_STRING = "service_id";
    /** service URL header name */
    public static final String SERVICE_URL_STRING = "service_url";
    /** environment tag header name */
    public static final String ENV_TAG_STRING = "env_tag";
    /** hash key header name */
    public static final String HASH_KEY_STRING = "hash_key";
    /** https protocol constant string */
    public static final String HTTPS = "https";
    /** header constant string */
    public static final String HEADER = "header";
    /** protocol constant string */
    public static final String PROTOCOL = "protocol";

    // Encode and decode for gzip and deflate
    /** gzip encoding constant string */
    public static final String ENCODE_GZIP = "gzip";
    /** deflate encoding constant string */
    public static final String ENCODE_DEFLATE = "deflate";

    //Rate Limit
    /** RateLimit-Limit header */
    public static final String RATELIMIT_LIMIT = "RateLimit-Limit";
    /** RateLimit-Remaining header */
    public static final String RATELIMIT_REMAINING = "RateLimit-Remaining";
    /** RateLimit-Reset header */
    public static final String RATELIMIT_RESET = "RateLimit-Reset";
    /** Retry-After header */
    public static final String RETRY_AFTER = "Retry-After";

    // rule loader
    /** rule ID constant */
    public static final String RULE_ID = "ruleId";

    // framework
    /** light-4j framework name */
    public static final String LIGHT_4J = "Light4j";
    /** spring boot framework name */
    public static final String SPRING_BOOT = "SpringBoot";

    // plugin error message
    /** error message constant */
    public static final String ERROR_MESSAGE = "errorMessage";

    // token exchange type
    /** CCAC token exchange type */
    public static final String TOKEN_EX_TYPE_CCAC = "ccac"; // client credentials to authorization code.
    /** MSAL token exchange type */
    public static final String TOKEN_EX_TYPE_MSAL = "msal"; // Microsoft authentication library
}
