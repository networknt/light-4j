package com.networknt.client.http;

import com.networknt.httpstring.ContentType;

import java.io.Serializable;

public class BodyPart<T> implements Serializable {

    private final ContentType contentType;

    private final T body;

    public BodyPart(ContentType type, T body) {
        this.contentType = type;
        this.body = body;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public T getBody() {
        return body;
    }
}