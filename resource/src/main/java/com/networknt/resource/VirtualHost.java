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

package com.networknt.resource;

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

public class VirtualHost {
    @StringField(
            configFieldName = "path",
            externalizedKeyName = "path",
            description = "The path of the virtual host",
            defaultValue = "/"
    )
    String path;

    @StringField(
            configFieldName = "domain",
            externalizedKeyName = "domain",
            description = "The domain of the virtual host",
            defaultValue = "localhost"
    )
    String domain;

    @StringField(
            configFieldName = "base",
            externalizedKeyName = "base",
            description = "The base directory of the virtual host",
            defaultValue = "/opt/light-4j/public"
    )
    String base;

    @IntegerField(
            configFieldName = "transferMinSize",
            externalizedKeyName = "transferMinSize",
            description = "The minimum size of the file to be transferred",
            defaultValue = "1024"
    )
    int transferMinSize;

    @BooleanField(
            configFieldName = "directoryListingEnabled",
            externalizedKeyName = "directoryListingEnabled",
            description = "If true, directory listing is enabled",
            defaultValue = "false"
    )
    boolean directoryListingEnabled;

    public VirtualHost() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public int getTransferMinSize() {
        return transferMinSize;
    }

    public void setTransferMinSize(int transferMinSize) {
        this.transferMinSize = transferMinSize;
    }

    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }

    public void setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
    }
}
