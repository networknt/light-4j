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

    /** Any content type */
    ANY_TYPE("*/*"),
    /** Application JSON content type */
    APPLICATION_JSON("application/json"),
    /** Text XML content type */
    XML("text/xml"),
    /** Application XML content type */
    APPLICATION_XML("application/xml"),
    /** Application YAML content type */
    APPLICATION_YAML("application/yaml"),
    /** Application form urlencoded content type */
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    /** Application PDF content type */
    APPLICATION_PDF("application/pdf"),
    /** Multipart mixed content type */
    MULTIPART_MIXED("multipart/mixed"),
    /** Multipart form data content type */
    MULTIPART_FORM_DATA("multipart/form-data"),
    /** Text plain content type */
    TEXT_PLAIN("text/plain"),
    /** Image PNG content type */
    IMAGE_PNG("image/png"),
    /** Image JPEG content type */
    IMAGE_JPEG("image/jpeg"),
    /** Image GIF content type */
    IMAGE_GIF("image/gif");

    private final String value;

    ContentType(String contentType) {
        this.value = contentType;
    }

    /**
     * Returns the string value of the content type.
     *
     * @return String value
     */
    public String value() {
        return this.value;
    }

    /**
     * Converts a string to a ContentType enum.
     *
     * @param value content type str eg: application/json
     * @return ContentType enum
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
