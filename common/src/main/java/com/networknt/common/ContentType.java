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
 * an enum for http Content-Type header
 *
 */
public enum ContentType {

    ANY_TYPE("*/*"),
    APPLICATION_JSON("application/json"),
    XML("text/xml"),
    APPLICATION_XML("application/xml"),
    APPLICATION_YAML("application/x-yaml"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    APPLICATION_PDF("application/pdf"),
    MULTIPART_MIXED("multipart/mixed"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    TEXT_PLAIN("text/plain"),
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_GIF("image/gif");

    private final String value;

    ContentType(String contentType) {
        this.value = contentType;
    }

    public String value() {
        return this.value;
    }

    /**
     * @param value content type str eg: application/json
     * @return ContentType
     */
    public static ContentType toContentType(String value) {
        for(ContentType v : values()){
            if(value.toUpperCase().contains(v.value().toUpperCase())) {
                return v;
            }
        }
        return ANY_TYPE;
    }
}
