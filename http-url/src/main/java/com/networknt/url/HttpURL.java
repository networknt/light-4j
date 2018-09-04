/* Copyright 2010-2018 Norconex Inc.
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

import com.networknt.utility.CharUtils;
import com.networknt.utility.RegExUtils;
import com.networknt.utility.StringUtils;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * This class act as a mutable URL, which could be a replacement
 * or "wrapper" to the {@link URL} class. It can also be used as a safer way
 * to build a {@link URL} or a {@link URI} instance as it will properly escape
 * appropriate characters before creating those.
 *
 * @author Pascal Essiembre
 */
//TODO rename MutableURL
public class HttpURL implements Serializable {

    private static final long serialVersionUID = -8886393027925815099L;

    /** Default URL HTTP Port. */
    public static final int DEFAULT_HTTP_PORT = 80;
    /** Default Secure URL HTTP Port. */
    public static final int DEFAULT_HTTPS_PORT = 443;

    /** Constant for "http" protocol. */
    public static final String PROTOCOL_HTTP = "http";
    /** Constant for "https" protocol. */
    public static final String PROTOCOL_HTTPS = "https";

    private QueryString queryString;
    private String host;
    private int port = -1;
    private String path;
    private String protocol;
    private final String encoding;
    private String fragment;

    /**
     * Creates a blank HttpURL using UTF-8 for URL encoding.
     */
    public HttpURL() {
        this("", null);
    }

    /**
     * Creates a new HttpURL from the URL object using UTF-8 for URL encoding.
     * @param url a URL
     */
    public HttpURL(URL url) {
        this(url.toString());
    }
    /**
     * Creates a new HttpURL from the URL string using UTF-8 for URL encoding.
     * @param url a URL
     */
    public HttpURL(String url) {
        this(url, null);
    }

    /**
     * Creates a new HttpURL from the URL object using the provided encoding
     * for URL encoding.
     * @param url a URL
     * @param encoding character encoding
     * @since 1.7.0
     */
    public HttpURL(URL url, String encoding) {
        this(url.toString(), encoding);
    }
    /**
     * Creates a new HttpURL from the URL string using the provided encoding
     * for URL encoding.
     * @param url a URL string
     * @param encoding character encoding
     * @since 1.7.0
     */
    public HttpURL(String url, String encoding) {
        if (StringUtils.isBlank(encoding)) {
            this.encoding = StandardCharsets.UTF_8.toString();
        } else {
            this.encoding = encoding;
        }

        String u = StringUtils.trimToEmpty(url);
        if (u.matches("[a-zA-Z][a-zA-Z0-9\\+\\-\\.]*:.*")) {
            URL urlwrap;
            try {
                urlwrap = new URL(u);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Could not interpret URL: " + u, e);
            }
            protocol = StringUtils.substringBefore(u, ":");
            host = urlwrap.getHost();
            port = urlwrap.getPort();
            if (port < 0) {
                if (u.toLowerCase().startsWith(PROTOCOL_HTTPS)) {
                    port = DEFAULT_HTTPS_PORT;
                } else if (u.toLowerCase().startsWith(PROTOCOL_HTTP)) {
                    port = DEFAULT_HTTP_PORT;
                }
            }
            path = urlwrap.getPath();
            fragment = urlwrap.getRef();
        } else {
            path = u.replaceFirst("^(.*?)([\\?\\#])(.*)", "$1");
            if (u.contains("#")) {
                fragment = u.replaceFirst("^(.*?)(\\#)(.*)", "$3");
            }
        }

        // Parameters
        if (u.contains("?")) {
            queryString = new QueryString(u, encoding);
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
     * Gets the URL path.
     * @return URL path
     */
    public String getPath() {
        return path;
    }
    /**
     * Sets the URL path.
     * @param path url path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the URL query string.
     * @return URL query string, or <code>null</code> if none
     */
    public QueryString getQueryString() {
        return queryString;
    }
    /**
     * Sets the URL query string.
     * @param queryString the query string
     */
    public void setQueryString(QueryString queryString) {
        this.queryString = queryString;
    }

    /**
     * Gets the host portion of the URL.
     * @return the host portion of the URL
     */
    public String getHost() {
        return host;
    }
    /**
     * Sets the host portion of the URL.
     * @param host the host portion of the URL
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets the protocol portion of the URL (e.g. http, https);
     * @return the protocol portion of the URL
     */
    public String getProtocol() {
        return protocol;
    }
    /**
     * Sets the protocol portion of the URL.
     * @param protocol the protocol portion of the URL
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    /**
     * Whether this URL is secure (e.g. https).
     * @return <code>true</code> if protocol is secure
     */
    public boolean isSecure() {
        return getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS);
    }

    /**
     * Gets the URL port. If the protocol is other than
     * <code>http</code> or <code>https</code>, the port is -1 when
     * not specified.
     * @return the URL port
     */
    public int getPort() {
        return port;
    }
    /**
     * Sets the URL port.
     * @param port the URL port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the URL fragment.
     * @return the fragment
     * @since 1.8.0
     */
    public String getFragment() {
        return fragment;
    }
    /**
     * Sets the URL fragment.
     * @param fragment the fragment to set
     * @since 1.8.0
     */
    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    /**
     * Gets the last URL path segment without the query string.
     * If there are segment to return,
     * an empty string will be returned instead.
     * @return the last URL path segment
     */
    public String getLastPathSegment() {
        if (StringUtils.isBlank(path)) {
            return StringUtils.EMPTY;
        }
        String segment = path;
        segment = StringUtils.substringAfterLast(segment, "/");
        return segment;
    }
    /**
     * Converts this HttpURL to a regular {@link URL}, making sure
     * appropriate characters are escaped properly.
     * @return a URL
     * @throws RuntimeException when URL is malformed
     */
    public URL toURL() {
        String url = toString();
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot convert to URL: " + url, e);
        }
    }

    /**
     * Gets the root of this HttpUrl. That is the left part of a URL up to
     * and including the host name. A <code>null</code> or empty string returns
     * a <code>null</code> document root.
     * @return left part of a URL up to (and including the host name
     * @throws RuntimeException when URL is malformed
     * @since 1.8.0
     */
    public String getRoot() {
        return getRoot(toString());
    }

    /**
     * Converts this HttpURL to a {@link URI}, making sure
     * appropriate characters are escaped properly.
     * @return a URI
     * @since 1.7.0
     * @throws RuntimeException when URL is malformed
     */
    public URI toURI() {
        String url = toString();
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot convert to URI: " + url, e);
        }
    }
    /**
     * <p>
     * Converts the supplied URL to a {@link URL}, making sure
     * appropriate characters are encoded properly using UTF-8. This method
     * is a short form of:<br>
     * <code>new HttpURL("http://example.com").toURL();</code>
     * </p>
     * @param url a URL string
     * @return a URL object
     * @since 1.7.0
     * @throws RuntimeException when URL is malformed
     */
    public static URL toURL(String url) {
        return new HttpURL(url).toURL();
    }
    /**
     * <p>Converts the supplied URL to a {@link URI}, making sure
     * appropriate characters are encoded properly using UTF-8. This method
     * is a short form of:<br>
     * <code>new HttpURL("http://example.com").toURI();</code>
     * </p>
     * @param url a URL string
     * @return a URI object
     * @since 1.7.0
     * @throws RuntimeException when URL is malformed
     */
    public static URI toURI(String url) {
        return new HttpURL(url).toURI();
    }

    /**
     * <p>Gets the root of a URL. That is the left part of a URL up to and
     * including the host name. A <code>null</code> or empty string returns
     * a <code>null</code> document root.
     * This method is a short form of:<br>
     * <code>new HttpURL("http://example.com/path").getRoot();</code>
     * </p>
     * @param url a URL string
     * @return left part of a URL up to (and including the host name
     * @since 1.8.0
     */
    public static String getRoot(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        return RegExUtils.replacePattern(url, "(.*?://.*?)([/?#].*)", "$1");
    }

    /**
     * Returns a string representation of this URL, properly encoded.
     * @return URL as a string
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (StringUtils.isNotBlank(protocol)) {
            b.append(protocol);
            b.append("://");
        }
        if (StringUtils.isNotBlank(host)) {
            b.append(host);
        }
        if (!isPortDefault() && port != -1) {
            b.append(':');
            b.append(port);
        }
        if (StringUtils.isNotBlank(path)) {
            // If no scheme/host/port, leave the path as is
            if (b.length() > 0 && !path.startsWith("/")) {
                b.append('/');
            }
            b.append(encodePath(path));
        }
        if (queryString != null && !queryString.isEmpty()) {
            b.append(queryString.toString());
        }
        if (fragment != null) {
            b.append("#");
            b.append(encodePath(fragment));
        }

        return b.toString();
    }

    /**
     * Whether this URL uses the default port for the protocol.  The default
     * port is 80 for "http" protocol, and 443 for "https". Other protocols
     * are not supported and this method will always return false
     * for them.
     * @return <code>true</code> if the URL is using the default port.
     * @since 1.8.0
     */
    public boolean isPortDefault() {
        return PROTOCOL_HTTPS.equalsIgnoreCase(protocol)
                && port == DEFAULT_HTTPS_PORT
                || PROTOCOL_HTTP.equalsIgnoreCase(protocol)
                && port == DEFAULT_HTTP_PORT;
    }

    /**
     * <p>URL-Encodes the query string portion of a URL. The entire
     * string supplied is assumed to be a query string.
     * @param queryString URL query string
     * @return encoded path
     * @since 1.8.0
     */
    public static String encodeQueryString(String queryString) {
        if (StringUtils.isBlank(queryString)) {
            return queryString;
        }
        return new QueryString(queryString).toString();
    }

    /**
     * <p>URL-Encodes a URL path.  The entire string supplied is assumed
     * to be a URL path. Unsafe characters are percent-encoded using UTF-8
     * (as specified by W3C standard).
     * @param path path portion of a URL
     * @return encoded path
     * @since 1.7.0
     */
    public static String encodePath(String path) {
        // Any characters that are not one of the following are
        // percent-encoded (including spaces):
        // a-z A-Z 0-9 . - _ ~ ! $ &amp; ' ( ) * + , ; = : @ / %
        if (StringUtils.isBlank(path)) {
            return path;
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : path.toCharArray()) {
            // Space to plus sign
            if (ch == ' ') {
                sb.append("%20");
                // Valid: keep it as is.
            } else if (CharUtils.isAsciiAlphanumeric(ch)
                    || ".-_~!$&'()*+,;=:@/%".indexOf(ch) != -1)  {
                sb.append(ch);
                // Invalid: encode it
            } else {
                byte[] bytes;
                bytes = Character.toString(ch).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    sb.append('%');
                    int upper = (((int) b) >> 4) & 0xf;
                    sb.append(Integer.toHexString(
                            upper).toUpperCase(Locale.US));
                    int lower = ((int) b) & 0xf;
                    sb.append(Integer.toHexString(
                            lower).toUpperCase(Locale.US));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Converts a relative URL to an absolute one, based on the supplied
     * base URL. The base URL is assumed to be a valid URL. Behavior
     * is unexpected when base URL is invalid.
     * @param baseURL URL to the reference is relative to
     * @param relativeURL the relative URL portion to transform to absolute
     * @return absolute URL
     * @since 1.8.0
     */
    public static String toAbsolute(String baseURL, String relativeURL) {
        String relURL = relativeURL;
        // Relative to protocol
        if (relURL.startsWith("//")) {
            return StringUtils.substringBefore(baseURL, "//") + "//"
                    + StringUtils.substringAfter(relURL, "//");
        }
        // Relative to domain name
        if (relURL.startsWith("/")) {
            return getRoot(baseURL) + relURL;
        }
        // Relative to full full page URL minus ? or #
        if (relURL.startsWith("?") || relURL.startsWith("#")) {
            // this is a relative url and should have the full page base
            return baseURL.replaceFirst("(.*?)([\\?\\#])(.*)", "$1") + relURL;
        }

        // Relative to last directory/segment
        if (!relURL.contains("://")) {
            String base = baseURL.replaceFirst("(.*?)([\\?\\#])(.*)", "$1");
            if (StringUtils.countMatches(base, '/') > 2) {
                base = base.replaceFirst("(.*/)(.*)", "$1");
            }
            if (base.endsWith("/")) {
                // This is a URL relative to the last URL segment
                relURL = base + relURL;
            } else {
                relURL = base + "/" + relURL;
            }
        }

        // Not detected as relative, so return as is
        return relURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpURL httpURL = (HttpURL) o;
        return port == httpURL.port &&
                Objects.equals(host, httpURL.host) &&
                Objects.equals(path, httpURL.path) &&
                Objects.equals(protocol, httpURL.protocol) &&
                Objects.equals(encoding, httpURL.encoding) &&
                Objects.equals(fragment, httpURL.fragment);
    }

    @Override
    public int hashCode() {

        return Objects.hash(host, port, path, protocol, encoding, fragment);
    }

}
