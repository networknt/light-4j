package com.networknt.client.http;

import com.networknt.common.ContentType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;



/**
 * The HttpRequestValue used to set the multipart  http request body
 * For some web server and legacy system, it request the body include the attachment. So the request body could has multiparts in it.
 * This object should only used for content in request body, please do not include http headers and cookies in it.
 * For Headers, please use clientRquest to set request header:
 *  request.getRequestHeaders().put(Headers.CONTENT_TYPE, FORM_DATA_TYPE);
 *  request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
 *
 * @author Gavin Chen
 */
public class HttpRequestValue implements Serializable {


    /** The body part map */
    private Map<String, BodyPart> bodyPartMap;
    //request overall content type
    /** The request content type */
    private ContentType contentType;

    /**
     * Default constructor.
     */
    public HttpRequestValue() {
        this(null);
    }

    /**
     * Constructor with content type.
     * @param contentType the content type
     */
    public HttpRequestValue(ContentType contentType) {
        this(contentType, null);
    }

    /**
     * Constructor with content type and body parts.
     * @param contentType the content type
     * @param bodyPartMap the map of body parts
     */
    public HttpRequestValue(ContentType contentType,  Map<String, BodyPart> bodyPartMap) {
        this.bodyPartMap = bodyPartMap;
    }

    /**
     * Set the body parts.
     * @param bodyPartMap the map of body parts
     */
    public void setBody(Map<String, BodyPart> bodyPartMap) {
        this.bodyPartMap = bodyPartMap;
    }


    /**
     * Get the body parts.
     * @return the map of body parts
     */
    public Map<String, BodyPart> getBody() {
        return this.bodyPartMap;
    }

    /**
     * Get a specific body part by key.
     * @param key the key
     * @return the body part, or null if not found
     */
    public  BodyPart getBody(String key) {
        return (this.bodyPartMap==null? null : this.bodyPartMap.get(key) );
    }

    /**
     * Indicates whether this entity has a body part by the key.
     * @param key the key
     * @return true if has body
     */
    public boolean hasBody(String key) {
        return (this.bodyPartMap==null? false : this.bodyPartMap.containsKey(key) );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("<");

        builder.append(',');
        Map<String, BodyPart> body = getBody();
        if (body != null) {
            builder.append(body);
            builder.append(',');
        }
        builder.append('>');
        return builder.toString();
    }

    /**
     * Create a builder.
     * @return the builder
     */
    public static HttpRequestValue.DefaultRequestValueBuilder builder() {
        return new HttpRequestValue.DefaultRequestValueBuilder();
    }

    /**
     * Create a builder with content type.
     * @param contentType the content type
     * @return the builder
     */
    public static HttpRequestValue.DefaultRequestValueBuilder builder(ContentType contentType) {
        return new HttpRequestValue.DefaultRequestValueBuilder(contentType);
    }

    /**
     * Builder for HttpRequestValue.
     */
    public static class DefaultRequestValueBuilder {
        private Map<String, BodyPart> mappings = new HashMap();
        private ContentType contentType;

        /**
         * Default constructor.
         */
        public DefaultRequestValueBuilder() {
        }

        /**
         * Constructor with content type.
         * @param contentType the content type
         */
        public DefaultRequestValueBuilder(ContentType contentType) {
            this.contentType = contentType;
        }

        /**
         * Add a body part.
         * @param name the name
         * @param type the content type
         * @param body the body object
         * @return the builder
         */
        public HttpRequestValue.DefaultRequestValueBuilder with(String name, ContentType type, Object body) {
            this.mappings.put(name, new BodyPart(type, body));
            return this;
        }

        /**
         * Build the HttpRequestValue.
         * @return the HttpRequestValue
         */
        public HttpRequestValue build() {
            return new HttpRequestValue(contentType, mappings);
        }
    }
}
