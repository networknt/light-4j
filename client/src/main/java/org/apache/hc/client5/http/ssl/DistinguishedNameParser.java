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

package org.apache.hc.client5.http.ssl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.ParserCursor;
import org.apache.hc.core5.http.message.TokenParser;
import org.apache.hc.core5.util.CharArrayBuffer;

final class DistinguishedNameParser {

    public final static DistinguishedNameParser INSTANCE = new DistinguishedNameParser();

    private static final BitSet EQUAL_OR_COMMA_OR_PLUS      = TokenParser.INIT_BITSET('=', ',', '+');
    private static final BitSet COMMA_OR_PLUS               = TokenParser.INIT_BITSET(',', '+');

    private final TokenParser tokenParser;

    DistinguishedNameParser() {
        this.tokenParser = new InternalTokenParser();
    }

    private String parseToken(final CharArrayBuffer buf, final ParserCursor cursor, final BitSet delimiters) {
        return tokenParser.parseToken(buf, cursor, delimiters);
    }

    private String parseValue(final CharArrayBuffer buf, final ParserCursor cursor, final BitSet delimiters) {
        return tokenParser.parseValue(buf, cursor, delimiters);
    }

    private NameValuePair parseParameter(final CharArrayBuffer buf, final ParserCursor cursor) {
        final String name = parseToken(buf, cursor, EQUAL_OR_COMMA_OR_PLUS);
        if (cursor.atEnd()) {
            return new BasicNameValuePair(name, null);
        }
        final int delim = buf.charAt(cursor.getPos());
        cursor.updatePos(cursor.getPos() + 1);
        if (delim == ',') {
            return new BasicNameValuePair(name, null);
        }
        final String value = parseValue(buf, cursor, COMMA_OR_PLUS);
        if (!cursor.atEnd()) {
            cursor.updatePos(cursor.getPos() + 1);
        }
        return new BasicNameValuePair(name, value);
    }

    List<NameValuePair> parse(final CharArrayBuffer buf, final ParserCursor cursor) {
        final List<NameValuePair> params = new ArrayList<>();
        tokenParser.skipWhiteSpace(buf, cursor);
        while (!cursor.atEnd()) {
            final NameValuePair param = parseParameter(buf, cursor);
            params.add(param);
        }
        return params;
    }

    List<NameValuePair> parse(final String s) {
        if (s == null) {
            return null;
        }
        final CharArrayBuffer buffer = new CharArrayBuffer(s.length());
        buffer.append(s);
        final ParserCursor cursor = new ParserCursor(0, s.length());
        return parse(buffer, cursor);
    }

    static class InternalTokenParser extends TokenParser {

        @Override
        public void copyUnquotedContent(
                final CharSequence buf,
                final ParserCursor cursor,
                final BitSet delimiters,
                final StringBuilder dst) {
            int pos = cursor.getPos();
            final int indexFrom = cursor.getPos();
            final int indexTo = cursor.getUpperBound();
            boolean escaped = false;
            for (int i = indexFrom; i < indexTo; i++, pos++) {
                final char current = buf.charAt(i);
                if (escaped) {
                    dst.append(current);
                    escaped = false;
                } else {
                    if ((delimiters != null && delimiters.get(current))
                            || TokenParser.isWhitespace(current) || current == '\"') {
                        break;
                    } else if (current == '\\') {
                        escaped = true;
                    } else {
                        dst.append(current);
                    }
                }
            }
            cursor.updatePos(pos);
        }
    }

}

