package com.networknt.httpstring;

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

    public static ContentType toContentType(String value) {
        for(ContentType v : values()){
            if(value.toUpperCase().contains(v.value().toUpperCase())) {
                return v;
            }
        }
        return ANY_TYPE;
    }
}
