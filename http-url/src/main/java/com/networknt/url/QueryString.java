/* Copyright 2010-2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.url;

import com.networknt.utility.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Provides utility methods for getting and setting attributes on
 * a URL query string.
 *
 * This is rewrite from commons-lang to remove extra dependencies.
 *
 * <br><br>
 * <b>Since 1.4</b>, query string parameters are stored and returned in the
 * order they were provided.
 * @author Pascal Essiembre
 */
public class QueryString {

    private static final long serialVersionUID = 1744232652147275170L;

    private final String encoding;
    private Map<String, List<String>> parameters = new HashMap<>();

    /**
     * Constructor.
     */
    public QueryString() {
        this(StringUtils.EMPTY, StandardCharsets.UTF_8.toString());
    }

    /**
     * Default URL character encoding is UTF-8.
     * @param urlWithQueryString a URL from which to extract a query string.
     */
    public QueryString(URL urlWithQueryString) {
        this(urlWithQueryString.toString(), null);
    }
    /**
     * Constructor.
     * @param urlWithQueryString a URL from which to extract a query string.
     * @param encoding character encoding
     */
    public QueryString(URL urlWithQueryString, String encoding) {
        this(urlWithQueryString.toString(), encoding);
    }
    /**
     * Constructor.   Default URL character encoding is UTF-8.
     * It is possible to only supply a query string as opposed to an
     * entire URL.
     * Key and values making up a query string are assumed to be URL-encoded.
     * Will throw a {@link RuntimeException} if UTF-8 encoding is not supported.
     * @param urlWithQueryString a URL from which to extract a query string.
     */
    public QueryString(String urlWithQueryString) {
        this(urlWithQueryString, null);
    }
    /**
     * Constructor.
     * It is possible to only supply a query string as opposed to an
     * entire URL.
     * Key and values making up a query string are assumed to be URL-encoded.
     * Will throw a {@link RuntimeException} if the supplied encoding is
     * unsupported or invalid.
     * @param urlWithQueryString a URL from which to extract a query string.
     * @param encoding character encoding
     */
    public QueryString(String urlWithQueryString, String encoding) {
        if (StringUtils.isBlank(encoding)) {
            this.encoding = StandardCharsets.UTF_8.toString();
        } else {
            this.encoding = encoding;
        }
        String paramString = urlWithQueryString;
        if (paramString.contains("?")) {
            paramString = StringUtils.substringBefore(paramString, "#");
            paramString = paramString.replaceAll("(.*?)(\\?)(.*)", "$3");
        }
        String[] paramParts = paramString.split("\\&");
        for (int i = 0; i < paramParts.length; i++) {
            String paramPart = paramParts[i];
            if (StringUtils.isBlank(paramPart)) {
                continue;
            }
            String key;
            String value;
            if (paramPart.contains("=")) {
                key = StringUtils.substringBefore(paramPart, "=");
                value = StringUtils.substringAfter(paramPart, "=");
            } else {
                key = paramPart;
                value = StringUtils.EMPTY;
            }
            try {
                addString(URLDecoder.decode(key, this.encoding),
                        URLDecoder.decode(value, this.encoding));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(
                        "Cannot URL-decode query string (key="
                                + key + "; value=" + value + ").", e);
            }
        }
    }

    /**
     * Gets the character encoding. Default is UTF-8.
     * @return character encoding
     * @since 1.7.0
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Convert this <code>QueryString</code> to a URL-encoded string
     * representation that can be appended as is to a URL with no query string.
     */
    @Override
    public synchronized String toString() {
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        char sep = '?';
        for (String key : parameters.keySet()) {
            for (String value : parameters.get(key)) {
                b.append(sep);
                sep = '&';
                try {
                    b.append(URLEncoder.encode(key, encoding));
                    b.append('=');
                    b.append(URLEncoder.encode(value, encoding));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(
                            "Cannot URL-encode query string (key="
                                    + key + "; value=" + value + ").", e);
                }
            }
        }
        return b.toString();
    }

    /**
     * Apply this url QueryString on the given URL. If a query string already
     * exists, it is replaced by this one.
     * @param url the URL to apply this query string.
     * @return url with query string added
     */
    public String applyOnURL(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return StringUtils.substringBefore(url, "?") + toString();
    }
    /**
     * Apply this url QueryString on the given URL. If a query string already
     * exists, it is replaced by this one.
     * @param url the URL to apply this query string.
     * @return url with query string added
     */
    public URL applyOnURL(URL url) {
        if (url == null) {
            return url;
        }
        try {
            return new URL(applyOnURL(url.toString()));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot applyl query string to: " + url, e);
        }
    }

    /**
     * Adds one or multiple string values.
     * Adding a single <code>null</code> value has no effect.
     * When adding multiple values, <code>null</code> values are converted
     * to blank strings.
     * @param key the key of the value to set
     * @param values the values to set
     */
    public final void addString(String key, String... values) {
        if (values == null || Array.getLength(values) == 0) {
            return;
        }
        List<String> list = parameters.get(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.addAll(Arrays.asList(values));
        parameters.put(key, list);
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }
}
