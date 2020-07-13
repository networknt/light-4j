package com.networknt.body;

/**
 * This is help class for the  List of content type
 *  Default is "application/json"
 * Created by Gavin Chen .
 */
public class ContentType {

    protected static final String WILDCARD_TYPE = "*";

    private final String type;

    private final String subtype;

    public ContentType(String type) {
        this(type, WILDCARD_TYPE);
    }

    public ContentType(String type, String subtype) {
        this.type = type;
        this.subtype = subtype;
    }




    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }
}
