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

package com.networknt.hmac.config;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;

import java.util.ArrayList;
import java.util.List;

/** Serializable, secret-free configuration for one HMAC provider profile. */
public class HmacProfileConfig {
    public static final int DEFAULT_MAX_BODY_BYTES = 16 * 1024 * 1024;

    @StringField(configFieldName = "signedInput", minLength = 1, pattern = "^rawBody$", defaultValue = "rawBody", description = "Optional; defaults to rawBody, the only supported signed input. Verify the exact received body bytes without parsing or normalization.")
    private String signedInput = "rawBody";

    @StringField(configFieldName = "algorithm", minLength = 1, pattern = "^hmacSha256$", defaultValue = "hmacSha256", description = "Optional; defaults to hmacSha256, the only supported algorithm.")
    private String algorithm = "hmacSha256";

    @ArrayField(configFieldName = "allowedMethods", items = String.class, defaultValue = "[\"POST\"]", uniqueItems = true, itemsPattern = "^([pP][oO][sS][tT]|[pP][uU][tT]|[pP][aA][tT][cC][hH])$", description = "Optional; omitted, null or empty defaults to [POST]. Unique subset of POST, PUT and PATCH; method names are normalized to uppercase.")
    private List<String> allowedMethods = new ArrayList<>(List.of("POST"));

    @StringField(configFieldName = "signatureHeader", minLength = 1, pattern = "^[!#$%&'*+.^_`|~0-9A-Za-z-]+$", description = "Required nonblank HTTP header name carrying the signature. GitHub uses X-Hub-Signature-256. Header names are case-insensitive.")
    private String signatureHeader;

    @StringField(configFieldName = "signaturePrefix", defaultValue = "", description = "Optional; defaults to an empty string. Exact case-sensitive prefix removed before decoding; GitHub uses sha256=. Characters below U+0020 are forbidden; DEL and C1 characters are not rejected by the current runtime.")
    private String signaturePrefix = "";

    @StringField(configFieldName = "signatureEncoding", minLength = 1, pattern = "^([hH][eE][xX]|[bB][aA][sS][eE]64)$", defaultValue = "hex", description = "Optional; defaults to hex. Supports hex or base64, normalized to lowercase.")
    private String signatureEncoding = "hex";

    @IntegerField(configFieldName = "maxBodyBytes", defaultValue = "16777216", min = 1, description = "Optional positive byte limit; defaults to 16777216 (16 MiB). Request injection must have enough exact-body buffering capacity.")
    private int maxBodyBytes = DEFAULT_MAX_BODY_BYTES;

    @ObjectField(configFieldName = "secrets", ref = HmacSecretsConfig.class, description = "Required secret source: configure bySelector or defaultEnvNames. Store environment-variable names only, never secret values.")
    private HmacSecretsConfig secrets = new HmacSecretsConfig();

    @ObjectField(configFieldName = "replay", ref = HmacReplayConfig.class, description = "Optional replay suppression policy; disabled by default. Storage implementation is selected in service.yml, not here.")
    private HmacReplayConfig replay = new HmacReplayConfig();

    public String getSignedInput() {
        return signedInput;
    }

    public void setSignedInput(String signedInput) {
        this.signedInput = signedInput;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {
        this.signatureHeader = signatureHeader;
    }

    public String getSignaturePrefix() {
        return signaturePrefix;
    }

    public void setSignaturePrefix(String signaturePrefix) {
        this.signaturePrefix = signaturePrefix;
    }

    public String getSignatureEncoding() {
        return signatureEncoding;
    }

    public void setSignatureEncoding(String signatureEncoding) {
        this.signatureEncoding = signatureEncoding;
    }

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public HmacSecretsConfig getSecrets() {
        return secrets;
    }

    public void setSecrets(HmacSecretsConfig secrets) {
        this.secrets = secrets;
    }

    public HmacReplayConfig getReplay() {
        return replay;
    }

    public void setReplay(HmacReplayConfig replay) {
        this.replay = replay;
    }
}
