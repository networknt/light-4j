package com.networknt.httpstring;

import io.opentracing.Span;
import io.opentracing.Tracer;
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
}
