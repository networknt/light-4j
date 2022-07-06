/*-
 * ========================LICENSE_START=================================
 * restheart-commons
 * %%
 * Copyright (C) 2019 - 2022 SoftInstigate
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package com.networknt.handler;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class BuffersUtils {
    public static final int MAX_CONTENT_SIZE = 16 * 1024 * 1024; // 16Mbyte

    private static final Logger LOGGER = LoggerFactory.getLogger(BuffersUtils.class);

    /**
     * @param srcs
     * @return
     * @throws IOException
     */
    public static ByteBuffer toByteBuffer(final PooledByteBuffer[] srcs) throws IOException {
        if (srcs == null) {
            return null;
        }

        ByteBuffer dst = ByteBuffer.allocate(MAX_CONTENT_SIZE);

        for (PooledByteBuffer src : srcs) {
            if (src != null) {
                final ByteBuffer srcBuffer = src.getBuffer();

                if (srcBuffer.remaining() > dst.remaining()) {
                    LOGGER.error("Request content exceeeded {} bytes limit",
                            MAX_CONTENT_SIZE);
                    throw new IOException("Request content exceeeded "
                            + MAX_CONTENT_SIZE + " bytes limit");
                }

                if (srcBuffer.hasRemaining()) {
                    Buffers.copy(dst, srcBuffer);

                    // very important, I lost a day for this!
                    srcBuffer.flip();
                }
            }
        }

        return dst.flip();
    }

    public static byte[] toByteArray(final PooledByteBuffer[] srcs) throws IOException {
        ByteBuffer content = toByteBuffer(srcs);

        byte[] ret = new byte[content.limit()];

        content.get(ret);

        return ret;
    }

    public static String toString(final PooledByteBuffer[] srcs, Charset cs) throws IOException {
        return new String(toByteArray(srcs), cs);
    }

    public static String toString(final byte[] src, Charset cs) throws IOException {
        return new String(src, cs);
    }

    /**
     * transfer the src data to the pooled buffers overwriting the exising data
     *
     * @param src
     * @param dest
     * @param exchange
     * @return
     */
    public static int transfer(final ByteBuffer src, final PooledByteBuffer[] dest, HttpServerExchange exchange) {
        int copied = 0;
        int pidx = 0;

        //src.rewind();
        while (src.hasRemaining() && pidx < dest.length) {
            ByteBuffer _dest;

            if (dest[pidx] == null) {
                dest[pidx] = exchange.getConnection().getByteBufferPool().allocate();
                _dest = dest[pidx].getBuffer();
            } else {
                _dest = dest[pidx].getBuffer();
                _dest.clear();
            }

            copied += Buffers.copy(_dest, src);

            // very important, I lost a day for this!
            _dest.flip();

            pidx++;
        }

        // clean up remaining destination buffers
        while (pidx < dest.length) {
            dest[pidx] = null;
            pidx++;
        }

        return copied;
    }

    public static void dump(String msg, PooledByteBuffer[] data) {
        int nbuf = 0;
        for (PooledByteBuffer dest : data) {
            if (dest != null) {
                ByteBuffer src = dest.getBuffer();
                StringBuilder sb = new StringBuilder();

                try {
                    Buffers.dump(src, sb, 2, 2);
                    LOGGER.debug("{} buffer #{}:\n{}", msg, nbuf, sb);
                } catch (IOException ie) {
                    LOGGER.debug("failed to dump buffered content", ie);
                }
            }
            nbuf++;
        }
    }

    /**
     * append the src data to the pooled buffers
     *
     * @param src
     * @param dest
     * @param exchange
     * @return
     */
    public static int append(final ByteBuffer src, final PooledByteBuffer[] dest, HttpServerExchange exchange) {
        int copied = 0;
        int pidx = 0;

        src.rewind();
        while (src.hasRemaining() && pidx < dest.length) {
            ByteBuffer _dest;

            if (dest[pidx] == null) {
                dest[pidx] = exchange.getConnection().getByteBufferPool().allocate();
                _dest = dest[pidx].getBuffer();
            } else {
                _dest = dest[pidx].getBuffer();
                _dest.position(_dest.limit());
            }

            copied += Buffers.copy(_dest, src);

            // very important, I lost a day for this!
            _dest.flip();

            pidx++;
        }

        // clean up remaining destination buffers
        while (pidx < dest.length) {
            dest[pidx] = null;
            pidx++;
        }

        return copied;
    }

    public static int transfer(final PooledByteBuffer[] src, final PooledByteBuffer[] dest, final HttpServerExchange exchange) {
        int copied = 0;
        int idx = 0;

        while (idx < src.length && idx < dest.length) {
            if (src[idx] != null) {
                if (dest[idx] == null) {
                    dest[idx] = exchange.getConnection().getByteBufferPool().allocate();
                }

                ByteBuffer _dest = dest[idx].getBuffer();
                ByteBuffer _src = src[idx].getBuffer();

                copied += Buffers.copy(_dest, _src);

                // very important, I lost a day for this!
                _dest.flip();
                _src.flip();
            }

            idx++;
        }

        // clean up remaining destination buffers
        while (idx < dest.length) {
            dest[idx] = null;
            idx++;
        }

        return copied;
    }
}
