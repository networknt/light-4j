package com.networknt.client.http;

import com.networknt.common.ContentType;

import java.io.Serializable;

/**
 * This class represents a part of a multipart HTTP body.
 * It contains the content type and the body of the part.
 *
 * @param <T> the type of the body
 */
public class BodyPart<T> implements Serializable {

    /** The content type */
    private final ContentType contentType;

    /** The body */
    private final T body;

    /**
     * Constructs a BodyPart with the given content type and body.
     * @param type the content type of the body part
     * @param body the body content
     */
    public BodyPart(ContentType type, T body) {
        this.contentType = type;
        this.body = body;
    }

    /**
     * Returns the content type of the body part.
     * @return the content type
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Returns the body content.
     * @return the body content
     */
    public T getBody() {
        return body;
    }
}
