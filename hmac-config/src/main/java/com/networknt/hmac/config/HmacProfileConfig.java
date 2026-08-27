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

    @StringField(configFieldName = "signedInput", defaultValue = "rawBody")
    private String signedInput = "rawBody";

    @StringField(configFieldName = "algorithm", defaultValue = "hmacSha256")
    private String algorithm = "hmacSha256";

    @ArrayField(configFieldName = "allowedMethods", items = String.class, defaultValue = "[\"POST\"]", minItems = 1)
    private List<String> allowedMethods = new ArrayList<>(List.of("POST"));

    @StringField(configFieldName = "signatureHeader")
    private String signatureHeader;

    @StringField(configFieldName = "signaturePrefix", defaultValue = "")
    private String signaturePrefix = "";

    @StringField(configFieldName = "signatureEncoding", defaultValue = "hex")
    private String signatureEncoding = "hex";

    @IntegerField(configFieldName = "maxBodyBytes", defaultValue = "16777216", min = 1)
    private int maxBodyBytes = DEFAULT_MAX_BODY_BYTES;

    @ObjectField(configFieldName = "secrets", ref = HmacSecretsConfig.class)
    private HmacSecretsConfig secrets = new HmacSecretsConfig();

    @ObjectField(configFieldName = "replay", ref = HmacReplayConfig.class)
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
