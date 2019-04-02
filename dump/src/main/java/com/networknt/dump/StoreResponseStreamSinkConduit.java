/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.networknt.dump;

import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.xnio.conduits.AbstractStreamSinkConduit;
import org.xnio.conduits.StreamSinkConduit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * this class is basically the same as io.undertow.conduits.StoredResponseStreamSinkConduit
 * just to fix some problems
 */
public class StoreResponseStreamSinkConduit extends AbstractStreamSinkConduit<StreamSinkConduit> {

    public static final AttachmentKey<byte[]> RESPONSE = AttachmentKey.create(byte[].class);
    private ByteArrayOutputStream outputStream;
    private final HttpServerExchange exchange;

    public StoreResponseStreamSinkConduit(StreamSinkConduit next, HttpServerExchange exchange) {
        super(next);
        this.exchange = exchange;
        long length = exchange.getResponseContentLength();
        if (length <= 0L) {
            outputStream = new ByteArrayOutputStream();
        } else {
            if (length > Integer.MAX_VALUE) {
                throw UndertowMessages.MESSAGES.responseTooLargeToBuffer(length);
            }
            outputStream = new ByteArrayOutputStream((int) length);
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int start = src.position();
        int ret = super.write(src);
        for (int i = start; i < start + ret; ++i) {
            outputStream.write(src.get(i));
        }
        return ret;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offs, int len) throws IOException {
        int[] starts = new int[len];
        for (int i = 0; i < len; ++i) {
            starts[i] = srcs[i + offs].position();
        }
        long ret = super.write(srcs, offs, len);
        long rem = ret;

        for (int i = 0; i < len; ++i) {
            ByteBuffer buf = srcs[i + offs];
            int pos = starts[i];
            int limit = buf.limit();
            //buf.position() could be later than buf.limit() due to after read the last byte, position will still move to next, but it's now larger than limit.
            while (rem > 0 && pos <= buf.position() && pos < limit) {
                outputStream.write(buf.get(pos));
                pos++;
                rem--;
            }
        }
        return ret;
    }

    @Override
    public int writeFinal(ByteBuffer src) throws IOException {
        int start = src.position();
        int ret = super.writeFinal(src);
        //without changing ByteBuffer remaining, copy to outputStream
        for (int i = start; i < start + ret; ++i) {
            outputStream.write(src.get(i));
        }
        return ret;
    }

    @Override
    public long writeFinal(ByteBuffer[] srcs, int offs, int len) throws IOException {
        int[] starts = new int[len];
        long toWrite = 0;
        for (int i = 0; i < len; ++i) {
            starts[i] = srcs[i + offs].position();
            toWrite += srcs[i + offs].remaining();
        }
        long ret = super.write(srcs, offs, len);
        long rem = ret;

        for (int i = 0; i < len; ++i) {
            ByteBuffer buf = srcs[i + offs];
            int pos = starts[i];
            int limit = buf.limit();
            //buf.position() could be later than buf.limit() due to after read the last byte, position will still move to next, but it's now larger than limit.
            while (rem > 0 && pos <= buf.position() && pos < limit) {
                outputStream.write(buf.get(pos));
                pos++;
                rem--;
            }
        }
        return ret;
    }

    @Override
    public void terminateWrites() throws IOException {
        //after finish writes all through conduit, it will reach here, at this time, we put response info
        exchange.putAttachment(RESPONSE, outputStream.toByteArray());
        outputStream = null;
        super.terminateWrites();
    }
}
