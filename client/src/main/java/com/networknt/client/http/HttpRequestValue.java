package com.networknt.client.http;

import com.networknt.httpstring.ContentType;


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


    private Map<String, BodyPart> bodyPartMap;
    //request overall content type
    private ContentType contentType;

    public HttpRequestValue() {
        this(null);
    }

    public HttpRequestValue(ContentType contentType) {
        this(contentType, null);
    }
    public HttpRequestValue(ContentType contentType,  Map<String, BodyPart> bodyPartMap) {
        this.bodyPartMap = bodyPartMap;
    }

    public void setBody(Map<String, BodyPart> bodyPartMap) {
        this.bodyPartMap = bodyPartMap;
    }


    public Map<String, BodyPart> getBody() {
        return this.bodyPartMap;
    }

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

    public static HttpRequestValue.DefaultRequestValueBuilder builder() {
        return new HttpRequestValue.DefaultRequestValueBuilder();
    }

    public static HttpRequestValue.DefaultRequestValueBuilder builder(ContentType contentType) {
        return new HttpRequestValue.DefaultRequestValueBuilder(contentType);
    }

    public static class DefaultRequestValueBuilder {
        private Map<String, BodyPart> mappings = new HashMap();
        private ContentType contentType;

        public DefaultRequestValueBuilder() {
        }

        public DefaultRequestValueBuilder(ContentType contentType) {
            this.contentType = contentType;
        }
        public HttpRequestValue.DefaultRequestValueBuilder with(String name, ContentType type, Object body) {
            this.mappings.put(name, new BodyPart(type, body));
            return this;
        }

        public HttpRequestValue build() {
            return new HttpRequestValue(contentType, mappings);
        }
    }
}
