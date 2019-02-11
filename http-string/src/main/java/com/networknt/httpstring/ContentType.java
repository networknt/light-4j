package com.networknt.httpstring;

/**
 * a enum for http Content-Type header
 * currently only support 3 types JSON, XML AND *\/*
 */
public enum ContentType {
    APPLICATION_JSON("application/json"),
    XML("text/xml"),
    ANY_TYPE("*/*");

    private String value;

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
