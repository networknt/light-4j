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

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

/** Replay policy associated with one HMAC profile. */
public class HmacReplayConfig {
    public static final int DEFAULT_RETENTION_SECONDS = 604800;

    @BooleanField(configFieldName = "enabled", defaultValue = "false")
    private boolean enabled;

    @StringField(configFieldName = "idHeader", defaultValue = "")
    private String idHeader = "";

    @IntegerField(configFieldName = "retentionSeconds", defaultValue = "604800", min = 1)
    private int retentionSeconds = DEFAULT_RETENTION_SECONDS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIdHeader() {
        return idHeader;
    }

    public void setIdHeader(String idHeader) {
        this.idHeader = idHeader;
    }

    public int getRetentionSeconds() {
        return retentionSeconds;
    }

    public void setRetentionSeconds(int retentionSeconds) {
        this.retentionSeconds = retentionSeconds;
    }
}
