package com.networknt.httpstring;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.util.AttachmentKey;

import java.util.Map;

/**
 * This class allows the definition of attachment keys shared by multiple modules and thus eliminates dependencies between them
 *
 * @author ddobrin
 *
 */
public class AttachmentConstants {
    // The key to the root span for this particular request attachment in exchange.
    public static final AttachmentKey<Span> ROOT_SPAN = AttachmentKey.create(Span.class);
    public static final AttachmentKey<Tracer> EXCHANGE_TRACER = AttachmentKey.create(Tracer.class);
    // The key to the audit info attachment in exchange. Allow other handlers to set values.
    public static final AttachmentKey<Map> AUDIT_INFO = AttachmentKey.create(Map.class);
    public static final AttachmentKey<Object> REQUEST_BODY = AttachmentKey.create(Object.class);
    public static final AttachmentKey<String> REQUEST_BODY_STRING = AttachmentKey.create(String.class);
    public static final AttachmentKey<Object> RESPONSE_BODY = AttachmentKey.create(Object.class);
    public static final AttachmentKey<String> RESPONSE_BODY_STRING = AttachmentKey.create(String.class);

    public static final AttachmentKey<PooledByteBuffer[]> BUFFERED_RESPONSE_DATA_KEY = AttachmentKey.create(PooledByteBuffer[].class);
    public static final AttachmentKey<PooledByteBuffer[]> BUFFERED_REQUEST_DATA_KEY = AttachmentKey.create(PooledByteBuffer[].class);

}
