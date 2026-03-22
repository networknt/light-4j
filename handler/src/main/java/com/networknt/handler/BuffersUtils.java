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
 * Utility class for working with PooledByteBuffer and ByteBuffer.
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class BuffersUtils {
    /**
     * Default constructor for BuffersUtils.
     */
    public BuffersUtils() {
    }

    /**
     * Configuration for request injection, used for max buffers.
     */
    public static final RequestInjectionConfig config = RequestInjectionConfig.load();

    /**
     * Maximum content size allowed based on configuration.
     */
    public static final int MAX_CONTENT_SIZE = 16 * 1024 * config.getMaxBuffers(); // 16KB * maxBuffers

    private static final Logger LOG = LoggerFactory.getLogger(BuffersUtils.class);

    /**
     * Converts an array of PooledByteBuffers to a single ByteBuffer.
     * @param srcs An array of PooledByteBuffer to be converted.
     * @return a ByteBuffer containing the content of the srcs
     * @throws IOException If the content exceeds the MAX_CONTENT_SIZE
     */
    public static ByteBuffer toByteBuffer(final PooledByteBuffer[] srcs) throws IOException {
        if (srcs == null)
            return null;

        var dst = ByteBuffer.allocate(MAX_CONTENT_SIZE);

        for (PooledByteBuffer src : srcs) {

            if (src != null) {
                final var srcBuffer = src.getBuffer();

                if (srcBuffer.remaining() > dst.remaining()) {

                    if (LOG.isErrorEnabled())
                        LOG.error("Request content exceeded {} bytes limit", MAX_CONTENT_SIZE);

                    throw new IOException("Request content exceeded " + MAX_CONTENT_SIZE + " bytes limit");
                }

                if (srcBuffer.hasRemaining()) {
                    Buffers.copy(dst, srcBuffer);
                    srcBuffer.flip();
                }
            }
        }

        return dst.flip();
    }

    /**
     * Converts an array of PooledByteBuffers to a byte array.
     * @param src An array of PooledByteBuffer to be converted.
     * @return a byte array containing the content of the src
     * @throws IOException If the content exceeds the MAX_CONTENT_SIZE
     */
    public static byte[] toByteArray(final PooledByteBuffer[] src) throws IOException {
        ByteBuffer content = toByteBuffer(src);

        if (content == null)
            return new byte[]{};

        byte[] ret = new byte[content.remaining()];
        content.rewind();

        /* 'get' actually copies the bytes from the ByteBuffer to our destination 'ret' byte array. */
        content.get(ret);
        return ret;
    }

    /**
     * Returns the actual byte array of the PooledByteBuffer.
     *
     * @param src An array of PooledByteBuffer to get the byte array from.
     * @return a byte array containing the content of the src
     * @throws IOException If the content exceeds the MAX_CONTENT_SIZE
     */
    public static byte[] getByteArray(final PooledByteBuffer[] src) throws IOException {
        ByteBuffer content = toByteBuffer(src);

        if (content != null && content.hasArray())
            return content.array();

        return new byte[]{};
    }

    /**
     * Converts an array of PooledByteBuffers to a String using the specified charset.
     * @param srcs An array of PooledByteBuffer to be converted.
     * @param cs The Charset to be used for decoding.
     * @return The resulting String.
     * @throws IOException If an I/O error occurs.
     */
    public static String toString(final PooledByteBuffer[] srcs, Charset cs) throws IOException {
        return new String(toByteArray(srcs), cs);
    }

    /**
     * Converts an array of PooledByteBuffers to a String using the specified charset name.
     * @param srcs An array of PooledByteBuffer to be converted.
     * @param charsetName The name of the charset to be used for decoding.
     * @return The resulting String.
     * @throws IOException If an I/O error occurs.
     */
    public static String toString(final PooledByteBuffer[] srcs, String charsetName) throws IOException {
        return new String(toByteArray(srcs), charsetName);
    }

    /**
     * Converts a byte array to a String using the specified charset.
     * @param src The byte array to be converted.
     * @param cs The Charset to be used for decoding.
     * @return The resulting String.
     * @throws IOException If an I/O error occurs.
     */
    public static String toString(final byte[] src, Charset cs) throws IOException {
        return new String(src, cs);
    }

    /**
     * transfer the src data to the pooled buffers overwriting the exising data
     *
     * @param src ByteBuffer
     * @param dest PooledByteBuffer[]
     * @param exchange HttpServerExchange
     * @return int
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

    /**
     * Dumps the content of pooled buffers for debugging purposes.
     * @param msg A message to be prefixed to the dump output.
     * @param data An array of PooledByteBuffer to be dumped.
     */
    public static void dump(String msg, PooledByteBuffer[] data) {
        int nbuf = 0;

        for (PooledByteBuffer dest : data) {

            if (dest != null) {
                var src = dest.getBuffer();
                var sb = new StringBuilder();

                try {
                    Buffers.dump(src, sb, 2, 2);

                    if (LOG.isDebugEnabled())
                        LOG.debug("{} buffer #{}:\n{}", msg, nbuf, sb);

                } catch (IOException ie) {

                    if (LOG.isErrorEnabled())
                        LOG.error("failed to dump buffered content", ie);

                }
            }
            nbuf++;
        }
    }

    /**
     * append the src data to the pooled buffers
     *
     * @param src ByteBuffer
     * @param dest PooledByteBuffer[]
     * @param exchange HttpServerExchange
     * @return int
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

    /**
     * Transfers data from one array of pooled buffers to another.
     * @param src The source array of PooledByteBuffer.
     * @param dest The destination array of PooledByteBuffer.
     * @param exchange The current HttpServerExchange.
     * @return The number of bytes transferred.
     */
    public static int transfer(final PooledByteBuffer[] src, final PooledByteBuffer[] dest, final HttpServerExchange exchange) {
        int copied = 0;
        int idx = 0;

        while (idx < src.length && idx < dest.length) {

            if (src[idx] != null) {

                if (dest[idx] == null)
                    dest[idx] = exchange.getConnection().getByteBufferPool().allocate();

                var _dest = dest[idx].getBuffer();
                var _src = src[idx].getBuffer();

                copied += Buffers.copy(_dest, _src);

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
