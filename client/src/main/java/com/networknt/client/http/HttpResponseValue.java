package com.networknt.client.http;

import com.networknt.common.ContentType;
import com.networknt.status.HttpStatus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The HttpResponseValue used to set the multipart  http response body
 * This object should only used for content in response body, please do not include http headers in it.
 * For Headers, please use HttpServerExchange to set response  header:
 *  exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
 *
 * @author Gavin Chen
 */
public class HttpResponseValue implements Serializable{


    /** The body part map */
    private Map<String, BodyPart> bodyPartMap;
    /** The status */
    private final HttpStatus status;

    /**
     * Default constructor.
     */
    public HttpResponseValue() {
        this(null);
    }

    /**
     * Constructor with status.
     * @param status the HTTP status
     */
    public HttpResponseValue(HttpStatus status) {
        this(status, null);
    }

    /**
     * Constructor with status and body parts.
     * @param status the HTTP status
     * @param bodyPartMap the map of body parts
     */
    public HttpResponseValue(HttpStatus status, Map<String, BodyPart> bodyPartMap) {
        this.status = status;
        this.bodyPartMap = bodyPartMap;
    }

    /**
     * Set the body parts.
     * @param bodyPartMap the map of body parts
     */
    public void setBody(HashMap<String, BodyPart> bodyPartMap) {
        this.bodyPartMap = bodyPartMap;
    }

    /**
     * Get the status code.
     * @return the HTTP status
     */
    public HttpStatus getStatusCode() {
        return  this.status;
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
     * @param key String
     * @return boolean to indicate if body exists
     */
    public boolean hasBody(String key) {
        return (this.bodyPartMap==null? false : this.bodyPartMap.containsKey(key) );
    }


    /**
     * Create a builder.
     * @return the builder
     */
    public static HttpResponseValue.DefaultResponseValueBuilder builder() {
        return new HttpResponseValue.DefaultResponseValueBuilder();
    }

    /**
     * Create a builder with status.
     * @param status the HTTP status
     * @return the builder
     */
    public static HttpResponseValue.DefaultResponseValueBuilder builder(HttpStatus status) {
        return new HttpResponseValue.DefaultResponseValueBuilder(status);
    }

    /**
     * Builder for HttpResponseValue.
     */
    public static class DefaultResponseValueBuilder implements Serializable{
        /** The mappings */
        private Map<String, BodyPart> mappings = new HashMap();
        /** The status */
        private  HttpStatus status;

        /**
         * Default constructor.
         */
        public DefaultResponseValueBuilder() {
        }

        /**
         * Constructor with status.
         * @param status the HTTP status
         */
        public DefaultResponseValueBuilder(HttpStatus status) {
            this.status = status;
        }

        /**
         * Add a body part.
         * @param name the name
         * @param type the content type
         * @param body the body object
         * @return the builder
         */
        public HttpResponseValue.DefaultResponseValueBuilder with(String name, ContentType type, Object body) {
            this.mappings.put(name, new BodyPart(type, body));
            return this;
        }

        /**
         * Build the HttpResponseValue.
         * @return the HttpResponseValue
         */
        public HttpResponseValue build() {
            return new HttpResponseValue(status, this.mappings);
        }
    }
}
