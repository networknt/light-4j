package com.networknt.client.http;

import com.networknt.httpstring.ContentType;
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


    private Map<String, BodyPart> bodyPartMap;
    private final HttpStatus status;

    public HttpResponseValue() {
        this(null);
    }

    public HttpResponseValue(HttpStatus status) {
        this(status, null);
    }

    public HttpResponseValue(HttpStatus status, Map<String, BodyPart> bodyPartMap) {
        this.status = status;
        this.bodyPartMap = bodyPartMap;
    }

    public void setBody(HashMap<String, BodyPart> bodyPartMap) {
        this.bodyPartMap = bodyPartMap;
    }

    public HttpStatus getStatusCode() {
        return  this.status;
    }

    public Map<String, BodyPart> getBody() {
        return this.bodyPartMap;
    }

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


    public static HttpResponseValue.DefaultResponseValueBuilder builder() {
        return new HttpResponseValue.DefaultResponseValueBuilder();
    }

    public static HttpResponseValue.DefaultResponseValueBuilder builder(HttpStatus status) {
        return new HttpResponseValue.DefaultResponseValueBuilder(status);
    }

    public static class DefaultResponseValueBuilder implements Serializable{
        private Map<String, BodyPart> mappings = new HashMap();
        private  HttpStatus status;

        public DefaultResponseValueBuilder() {
        }

        public DefaultResponseValueBuilder(HttpStatus status) {
            this.status = status;
        }
        public HttpResponseValue.DefaultResponseValueBuilder with(String name, ContentType type, Object body) {
            this.mappings.put(name, new BodyPart(type, body));
            return this;
        }

        public HttpResponseValue build() {
            return new HttpResponseValue(status, this.mappings);
        }
    }
}
