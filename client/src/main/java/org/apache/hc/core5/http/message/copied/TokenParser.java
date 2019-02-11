/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.core5.http.message.copied;

import java.util.BitSet;

import org.apache.hc.core5.annotation.copied.Contract;
import org.apache.hc.core5.annotation.copied.ThreadingBehavior;
import org.apache.hc.core5.util.copied.Args;

/**
 * Low level parser for header field elements. The parsing routines of this class are designed
 * to produce near zero intermediate garbage and make no intermediate copies of input data.
 * <p>
 * This class is immutable and thread safe.
 *
 * @since 4.4
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class TokenParser {

    public static BitSet INIT_BITSET(final int ... b) {
        final BitSet bitset = new BitSet();
        for (final int aB : b) {
            bitset.set(aB);
        }
        return bitset;
    }

    /** US-ASCII CR, carriage return (13) */
    public static final char CR = '\r';

    /** US-ASCII LF, line feed (10) */
    public static final char LF = '\n';

    /** US-ASCII SP, space (32) */
    public static final char SP = ' ';

    /** US-ASCII HT, horizontal-tab (9) */
    public static final char HT = '\t';

    /** Double quote */
    public static final char DQUOTE = '\"';

    /** Backward slash / escape character */
    public static final char ESCAPE = '\\';

    public static boolean isWhitespace(final char ch) {
        return ch == SP || ch == HT || ch == CR || ch == LF;
    }

    public static final TokenParser INSTANCE = new TokenParser();

    public String parseToken(final CharSequence buf, final ParserCursor cursor, final BitSet delimiters) {
        Args.notNull(buf, "Char sequence");
        Args.notNull(cursor, "Parser cursor");
        final StringBuilder dst = new StringBuilder();
        boolean whitespace = false;
        while (!cursor.atEnd()) {
            final char current = buf.charAt(cursor.getPos());
            if (delimiters != null && delimiters.get(current)) {
                break;
            } else if (isWhitespace(current)) {
                skipWhiteSpace(buf, cursor);
                whitespace = true;
            } else {
                if (whitespace && dst.length() > 0) {
                    dst.append(' ');
                }
                copyContent(buf, cursor, delimiters, dst);
                whitespace = false;
            }
        }
        return dst.toString();
    }

    public String parseValue(final CharSequence buf, final ParserCursor cursor, final BitSet delimiters) {
        Args.notNull(buf, "Char sequence");
        Args.notNull(cursor, "Parser cursor");
        final StringBuilder dst = new StringBuilder();
        boolean whitespace = false;
        while (!cursor.atEnd()) {
            final char current = buf.charAt(cursor.getPos());
            if (delimiters != null && delimiters.get(current)) {
                break;
            } else if (isWhitespace(current)) {
                skipWhiteSpace(buf, cursor);
                whitespace = true;
            } else if (current == DQUOTE) {
                if (whitespace && dst.length() > 0) {
                    dst.append(' ');
                }
                copyQuotedContent(buf, cursor, dst);
                whitespace = false;
            } else {
                if (whitespace && dst.length() > 0) {
                    dst.append(' ');
                }
                copyUnquotedContent(buf, cursor, delimiters, dst);
                whitespace = false;
            }
        }
        return dst.toString();
    }

    /**
     * Skips semantically insignificant whitespace characters and moves the cursor to the closest
     * non-whitespace character.
     *
     * @param buf buffer with the sequence of chars to be parsed
     * @param cursor defines the bounds and current position of the buffer
     */
    public void skipWhiteSpace(final CharSequence buf, final ParserCursor cursor) {
        Args.notNull(buf, "Char sequence");
        Args.notNull(cursor, "Parser cursor");
        int pos = cursor.getPos();
        final int indexFrom = cursor.getPos();
        final int indexTo = cursor.getUpperBound();
        for (int i = indexFrom; i < indexTo; i++) {
            final char current = buf.charAt(i);
            if (!isWhitespace(current)) {
                break;
            }
            pos++;
        }
        cursor.updatePos(pos);
    }

    /**
     * Transfers content into the destination buffer until a whitespace character or any of
     * the given delimiters is encountered.
     *
     * @param buf buffer with the sequence of chars to be parsed
     * @param cursor defines the bounds and current position of the buffer
     * @param delimiters set of delimiting characters. Can be {@code null} if the value
     *  is delimited by a whitespace only.
     * @param dst destination buffer
     */
    public void copyContent(final CharSequence buf, final ParserCursor cursor, final BitSet delimiters,
            final StringBuilder dst) {
        Args.notNull(buf, "Char sequence");
        Args.notNull(cursor, "Parser cursor");
        Args.notNull(dst, "String builder");
        int pos = cursor.getPos();
        final int indexFrom = cursor.getPos();
        final int indexTo = cursor.getUpperBound();
        for (int i = indexFrom; i < indexTo; i++) {
            final char current = buf.charAt(i);
            if ((delimiters != null && delimiters.get(current)) || isWhitespace(current)) {
                break;
            }
            pos++;
            dst.append(current);
        }
        cursor.updatePos(pos);
    }

    /**
     * Transfers content into the destination buffer until a whitespace character,  a quote,
     * or any of the given delimiters is encountered.
     *
     * @param buf buffer with the sequence of chars to be parsed
     * @param cursor defines the bounds and current position of the buffer
     * @param delimiters set of delimiting characters. Can be {@code null} if the value
     *  is delimited by a whitespace or a quote only.
     * @param dst destination buffer
     */
    public void copyUnquotedContent(final CharSequence buf, final ParserCursor cursor,
            final BitSet delimiters, final StringBuilder dst) {
        Args.notNull(buf, "Char sequence");
        Args.notNull(cursor, "Parser cursor");
        Args.notNull(dst, "String builder");
        int pos = cursor.getPos();
        final int indexFrom = cursor.getPos();
        final int indexTo = cursor.getUpperBound();
        for (int i = indexFrom; i < indexTo; i++) {
            final char current = buf.charAt(i);
            if ((delimiters != null && delimiters.get(current))
                    || isWhitespace(current) || current == DQUOTE) {
                break;
            }
            pos++;
            dst.append(current);
        }
        cursor.updatePos(pos);
    }

    /**
     * Transfers content enclosed with quote marks into the destination buffer.
     *
     * @param buf buffer with the sequence of chars to be parsed
     * @param cursor defines the bounds and current position of the buffer
     * @param dst destination buffer
     */
    public void copyQuotedContent(final CharSequence buf, final ParserCursor cursor,
            final StringBuilder dst) {
        Args.notNull(buf, "Char sequence");
        Args.notNull(cursor, "Parser cursor");
        Args.notNull(dst, "String builder");
        if (cursor.atEnd()) {
            return;
        }
        int pos = cursor.getPos();
        int indexFrom = cursor.getPos();
        final int indexTo = cursor.getUpperBound();
        char current = buf.charAt(pos);
        if (current != DQUOTE) {
            return;
        }
        pos++;
        indexFrom++;
        boolean escaped = false;
        for (int i = indexFrom; i < indexTo; i++, pos++) {
            current = buf.charAt(i);
            if (escaped) {
                if (current != DQUOTE && current != ESCAPE) {
                    dst.append(ESCAPE);
                }
                dst.append(current);
                escaped = false;
            } else {
                if (current == DQUOTE) {
                    pos++;
                    break;
                }
                if (current == ESCAPE) {
                    escaped = true;
                } else if (current != CR && current != LF) {
                    dst.append(current);
                }
            }
        }
        cursor.updatePos(pos);
    }

}
