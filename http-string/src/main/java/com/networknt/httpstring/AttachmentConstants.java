package com.networknt.httpstring;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.undertow.util.AttachmentKey;

public class AttachmentConstants {
    // The key to the root span for this particular request attachment in exchange.
    public static final AttachmentKey<Span> ROOT_SPAN = AttachmentKey.create(Span.class);
    public static final AttachmentKey<Tracer> EXCHANGE_TRACER = AttachmentKey.create(Tracer.class);

}
