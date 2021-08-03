package com.networknt.client.model;

import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public enum HttpVerb {
    GET(Methods.GET),
    POST(Methods.POST),
    PUT(Methods.PUT),
    DELETE(Methods.DELETE),
    HEAD(Methods.HEAD),
    OPTIONS(Methods.OPTIONS),
    TRACE(Methods.TRACE),
    PATCH(Methods.PATCH),
    CONNECT(Methods.CONNECT);

    public final HttpString verbHttpString;

    HttpVerb(HttpString verb) {
        this.verbHttpString = verb;
    }
}