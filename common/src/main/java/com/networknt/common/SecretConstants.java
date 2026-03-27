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

package com.networknt.common;

/**
 * Constants for secret property names in different configuration files.
 */
public class SecretConstants {
    private SecretConstants() {
        throw new IllegalStateException("SecretConstants is a constants class");
    }

    /** Constant for server keystore password */
    public static final String SERVER_KEYSTORE_PASS = "serverKeystorePass";
    /** Constant for server key password */
    public static final String SERVER_KEY_PASS = "serverKeyPass";
    /** Constant for server truststore password */
    public static final String SERVER_TRUSTSTORE_PASS = "serverTruststorePass";
    /** Constant for client keystore password */
    public static final String CLIENT_KEYSTORE_PASS = "clientKeystorePass";
    /** Constant for client key password */
    public static final String CLIENT_KEY_PASS = "clientKeyPass";
    /** Constant for client truststore password */
    public static final String CLIENT_TRUSTSTORE_PASS = "clientTruststorePass";

    /** Constant for authorization code client secret */
    public static final String AUTHORIZATION_CODE_CLIENT_SECRET = "authorizationCodeClientSecret";
    /** Constant for client credentials client secret */
    public static final String CLIENT_CREDENTIALS_CLIENT_SECRET = "clientCredentialsClientSecret";
    /** Constant for refresh token client secret */
    public static final String REFRESH_TOKEN_CLIENT_SECRET = "refreshTokenClientSecret";
    /** Constant for key client secret */
    public static final String KEY_CLIENT_SECRET = "keyClientSecret";
    /** Constant for deref client secret */
    public static final String DEREF_CLIENT_SECRET = "derefClientSecret";

    /** Constant for consul token */
    public static final String CONSUL_TOKEN = "consulToken";
    /** Constant for email password */
    public static final String EMAIL_PASSWORD = "emailPassword";

}
