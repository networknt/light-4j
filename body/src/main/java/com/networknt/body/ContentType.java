package com.networknt.body;

/**
 * This is help class for the  List of content type
 *  Default is "application/json"
 * Created by Gavin Chen .
 */
public class ContentType {

    protected static final String WILDCARD_TYPE = "*";

    private final String type;


    public static final String ALL_VALUE = "*/*";

    public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

    public static final String APPLICATION_CBOR_VALUE = "application/cbor";

    public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

    public static final String APPLICATION_JSON_VALUE = "application/json";

    public static final String APPLICATION_PDF_VALUE = "application/pdf";

    public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";

    public static final String APPLICATION_XML_VALUE = "application/xml";

    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

    public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

    public static final String TEXT_HTML_VALUE = "text/html";

    public static final String TEXT_PLAIN_VALUE = "text/plain";

    public static final String TEXT_XML_VALUE = "text/xml";

    public static final String IMAGE_PNG_VALUE = "image/png";

    public static final String IMAGE_JPEG_VALUE = "image/jpeg";

    public static final String IMAGE_GIF_VALUE = "image/gif";

    public ContentType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


}
