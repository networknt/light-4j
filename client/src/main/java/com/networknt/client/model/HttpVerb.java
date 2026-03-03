package com.networknt.client.model;

import io.undertow.util.HttpString;
import io.undertow.util.Methods;

/**
 * HTTP verb enum.
 */
public enum HttpVerb {
    /** GET */
    GET(Methods.GET),
    /** POST */
    POST(Methods.POST),
    /** PUT */
    PUT(Methods.PUT),
    /** DELETE */
    DELETE(Methods.DELETE),
    /** HEAD */
    HEAD(Methods.HEAD),
    /** OPTIONS */
    OPTIONS(Methods.OPTIONS),
    /** TRACE */
    TRACE(Methods.TRACE),
    /** PATCH */
    PATCH(Methods.PATCH),
    /** CONNECT */
    CONNECT(Methods.CONNECT);

    /** The HttpString representation of the verb */
    public final HttpString verbHttpString;

    /**
     * Constructor.
     * @param verb the HttpString verb
     */
    HttpVerb(HttpString verb) {
        this.verbHttpString = verb;
    }
}
