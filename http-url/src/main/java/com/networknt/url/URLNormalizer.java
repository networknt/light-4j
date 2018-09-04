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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * The general idea behind URL normalization is to make different URLs
 * "equivalent" (i.e. eliminate URL variations pointing to the same resource).
 * To achieve this,
 * <code>URLNormalizer</code> takes a URL and modifies it to its
 * most basic or standard form (for the context in which it is used).
 * Of course <code>URLNormalizer</code> can simply be used as a generic
 * URL manipulation tool for your needs.
 * </p>
 * <p>
 * You would typically "build" your normalized URL by invoking each method
 * of interest, in the relevant order, using a similar approach:
 * </p>
 * <pre>
 * String url = "Http://Example.com:80//foo/index.html";
 * URL normalizedURL = new URLNormalizer(url)
 *         .lowerCaseSchemeHost()
 *         .removeDefaultPort()
 *         .removeDuplicateSlashes()
 *         .removeDirectoryIndex()
 *         .addWWW()
 *         .toURL();
 * System.out.println(normalizedURL.toString());
 * // Output: http://www.example.com/foo/</pre>
 * <p>
 * Several normalization methods implemented come from the
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard.
 * These standards and several more normalization techniques
 * are very well summarized on the Wikipedia article titled
 * <i><a href="http://en.wikipedia.org/wiki/URL_normalization">
 * URL Normalization</a></i>.
 * This class implements most normalizations described on that article and
 * borrows several of its examples, as well as a few additional ones.
 * </p>
 * <p>
 * The normalization methods available can be broken down into three
 * categories:
 * </p>
 *
 * <h3>Preserving Semantics</h3>
 * <p>
 * The following normalizations are part of the
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard
 * and should result in equivalent
 * URLs (one that identifies the same resource):
 * </p>
 * <ul>
 *   <li>{@link #lowerCaseSchemeHost()
 *       Convert scheme and host to lower case}</li>
 *   <li>{@link #upperCaseEscapeSequence()
 *       Convert escape sequence to upper case}</li>
 *   <li>{@link #decodeUnreservedCharacters()
 *       Decode percent-encoded unreserved characters}</li>
 *   <li>{@link #removeDefaultPort() Removing default ports}</li>
 *   <li>{@link #encodeNonURICharacters()
 *       URL-Encode non-ASCII characters}</li>
 *   <li>{@link #encodeSpaces() Encode spaces to plus sign}</li>
 * </ul>
 *
 * <h3>Usually Preserving Semantics</h3>
 * <p>
 * The following techniques will generate a semantically equivalent URL for
 * the majority of use cases but are not enforced as a standard.
 * </p>
 * <ul>
 *   <li>{@link #addTrailingSlash() Add trailing slash}</li>
 *   <li>{@link #removeDotSegments() Remove .dot segments}</li>
 * </ul>
 *
 * <h3>Not Preserving Semantics</h3>
 * <p>
 * These normalizations will fail to produce semantically equivalent URLs in
 * many cases.  They usually work best when you have a good understanding of
 * the web site behind the supplied URL and whether for that site,
 * which normalizations can be be considered to produce semantically equivalent
 * URLs or not.
 * </p>
 * <ul>
 *   <li>{@link #removeDirectoryIndex() Remove directory index}</li>
 *   <li>{@link #removeFragment() Remove fragment (#)}</li>
 *   <li>{@link #replaceIPWithDomainName() Replace IP with domain name}</li>
 *   <li>{@link #unsecureScheme() Unsecure schema (https &rarr; http)}</li>
 *   <li>{@link #secureScheme() Secure schema (http &rarr; https)}</li>
 *   <li>{@link #removeDuplicateSlashes() Remove duplicate slashes}</li>
 *   <li>{@link #removeWWW() Remove "www."}</li>
 *   <li>{@link #addWWW() Add "www."}</li>
 *   <li>{@link #sortQueryParameters() Sort query parameters}</li>
 *   <li>{@link #removeEmptyParameters() Remove empty query parameters}</li>
 *   <li>{@link #removeTrailingQuestionMark() Remove trailing question mark (?)}</li>
 *   <li>{@link #removeSessionIds() Remove session IDs}</li>
 * </ul>
 * <p>
 * Refer to each methods below for description and examples (or click on a
 * normalization name above).
 * </p>
 * @author Pascal Essiembre
 */
public class URLNormalizer implements Serializable {

    private static final long serialVersionUID = 7236478212865008971L;

    private static final Logger logger = LoggerFactory.getLogger(URLNormalizer.class);

    private static final Pattern PATTERN_PERCENT_ENCODED_CHAR =
            Pattern.compile("(%[0-9a-f]{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PATH_LAST_SEGMENT = Pattern.compile(
            "(.*/)(index\\.html|index\\.htm|index\\.shtml|index\\.php"
                    + "|default\\.html|default\\.htm|home\\.html|home\\.htm|index\\.php5"
                    + "|index\\.php4|index\\.php3|index\\.cgi|placeholder\\.html"
                    + "|default\\.asp)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DOMAIN = Pattern.compile(
            "^[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SCHEMA = Pattern.compile(
            "(.*?)(://.*)$",
            Pattern.CASE_INSENSITIVE);

    private String url;

    /**
     * Create a new <code>URLNormalizer</code> instance.
     * @param url the url to normalize
     */
    public URLNormalizer(URL url) {
        this(Objects.toString(url, null));
    }

    /**
     * <p>
     * Create a new <code>URLNormalizer</code> instance.
     * </p><p>
     * Since 1.8.0, spaces in URLs are no longer converted to + automatically.
     * Use {@link #encodeNonURICharacters()} or {@link #encodeSpaces()}.
     * </p>
     * @param url the url to normalize
     */
    public URLNormalizer(String url) {
        super();
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL argument cannot be null.");
        }
        this.url = url.trim();
        // Check it is a valid URL.
        try {
            new URL(this.url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }

    /**
     * Converts the scheme and host to lower case.<p>
     * <code>HTTP://www.Example.com/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer lowerCaseSchemeHost() {
        URL u = toURL();
        url = Pattern.compile(u.getProtocol(),
                Pattern.CASE_INSENSITIVE).matcher(url).replaceFirst(
                u.getProtocol().toLowerCase());
        url = Pattern.compile(u.getHost(),
                Pattern.CASE_INSENSITIVE).matcher(url).replaceFirst(
                u.getHost().toLowerCase());
        return this;
    }
    /**
     * Converts letters in URL-encoded escape sequences to upper case.<p>
     * <code>http://www.example.com/a%c2%b1b &rarr;
     *       http://www.example.com/a%C2%B1b</code>
     * @return this instance
     */
    public URLNormalizer upperCaseEscapeSequence() {
        if (url.contains("%")) {
            StringBuffer sb = new StringBuffer();
            Matcher m = PATTERN_PERCENT_ENCODED_CHAR.matcher(url);
            while (m.find()) {
                m.appendReplacement(sb, m.group(1).toUpperCase());
            }
            url = m.appendTail(sb).toString();
        }
        return this;
    }
    /**
     * Decodes percent-encoded unreserved characters.<p>
     * <code>http://www.example.com/%7Eusername/ &rarr;
     *       http://www.example.com/~username/</code>
     * @return this instance
     */
    public URLNormalizer decodeUnreservedCharacters() {
        if (url.contains("%")) {
            StringBuffer sb = new StringBuffer();
            Matcher m = PATTERN_PERCENT_ENCODED_CHAR.matcher(url);
            try {
                while (m.find()) {
                    String enc = m.group(1).toUpperCase();
                    if (isEncodedUnreservedCharacter(enc)) {
                        m.appendReplacement(sb, URLDecoder.decode(
                                enc, StandardCharsets.UTF_8.toString()));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                logger.debug("UTF-8 is not supported by your system. "
                        + "URL will remain unchanged:" + url, e);
            }
            url = m.appendTail(sb).toString();
        }
        return this;
    }

    /**
     * <p>
     * Encodes all characters that are not supported characters
     * in a URI (not to confuse with URL), as defined
     * by the <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a>
     * standard. This includes all non-ASCII characters.
     * </p>
     * <p>
     * Since this method also encodes spaces to the plus sign (+), there is
     * no need to also invoke {@link #encodeSpaces()}.
     * </p>
     * <code>http://www.example.com/^a [b]/ &rarr;
     *       http://www.example.com/%5Ea+%5Bb%5D/</code>
     * @return this instance
     * @since 1.8.0
     */
    public URLNormalizer encodeNonURICharacters() {
        url = toURI().toASCIIString();
        return this;
    }
    /**
     * <p>
     * Encodes space characters into plus signs (+) if they are part of the
     * query string. Spaces part of the URL path are percent-encoded to %20.
     * </p>
     * <p>
     * To encode all non-ASCII characters (including spaces), use
     * {@link #encodeNonURICharacters()} instead.
     * </p>
     * <code>http://www.example.com/a b c &rarr;
     *       http://www.example.com/a+b+c</code>
     * @return this instance
     * @since 1.8.0
     */
    public URLNormalizer encodeSpaces() {
        String path = StringUtils.substringBefore(url, "?");
        path = StringUtils.replace(path, " ", "%20", -1);
        String qs = StringUtils.substringAfter(url, "?");
        if (StringUtils.isNotBlank(qs)) {
            qs = StringUtils.replace(qs, " ", "+", -1);
            url = path + "?" + qs;
        } else {
            url = path;
        }
        return this;
    }

    /**
     * Removes the default port (80 for http, and 443 for https).<p>
     * <code>http://www.example.com:80/bar.html &rarr;
     *       http://www.example.com/bar.html</code>
     * @return this instance
     */
    public URLNormalizer removeDefaultPort() {
        URL u = toURL();
        if ("http".equalsIgnoreCase(u.getProtocol())
                && u.getPort() == HttpURL.DEFAULT_HTTP_PORT) {
            url = url.replaceFirst(":" + HttpURL.DEFAULT_HTTP_PORT, "");
        } else if ("https".equalsIgnoreCase(u.getProtocol())
                && u.getPort() == HttpURL.DEFAULT_HTTPS_PORT) {
            url = url.replaceFirst(":" + HttpURL.DEFAULT_HTTPS_PORT, "");
        }
        return this;
    }

    /**
     * <p>Adds a trailing slash (/) to a URL ending with a directory.  A URL is
     * considered to end with a directory if the last path segment,
     * before fragment (#) or query string (?), does not contain a dot,
     * typically representing an extension.</p>
     *
     * <p><b>Please Note:</b> URLs do not always denote a directory structure
     * and many URLs can qualify to this method without truly representing a
     * directory. Adding a trailing slash to these URLs could potentially break
     * its semantic equivalence.</p>
     * <code>http://www.example.com/alice &rarr;
     *       http://www.example.com/alice/</code>
     * @return this instance
     * @since 1.11.0 (renamed from "addTrailingSlash")
     */
    public URLNormalizer addDirectoryTrailingSlash() {
        String urlRoot = HttpURL.getRoot(url);
        String path = toURL().getPath();
        String urlRootAndPath = urlRoot + path;

        String name = StringUtils.substringAfterLast(path, "/");
        if (StringUtils.isNotBlank(name) && !name.contains(".")) {
            String newPath = path + "/";
            String newUrlRootAndPath = urlRoot + newPath;
            url = StringUtils.replaceOnce(
                    url, urlRootAndPath, newUrlRootAndPath);
        }
        return this;
    }

    /**
     * <p>Adds a trailing slash (/) right after the domain for URLs with no
     * path, before any fragment (#) or query string (?).</p>
     *
     * <p><b>Please Note:</b> Adding a trailing slash to URLs could
     * potentially break its semantic equivalence.</p>
     * <code>http://www.example.com &rarr;
     *       http://www.example.com/</code>
     * @return this instance
     * @since 1.12.0
     */
    public URLNormalizer addDomainTrailingSlash() {
        String urlRoot = HttpURL.getRoot(url);
        String path = toURL().getPath();
        if (StringUtils.isNotBlank(path)) {
            // there is a path so do nothing
            return this;
        }
        String urlRootAndPath = urlRoot + "/";
        url = StringUtils.replaceOnce(url, urlRoot, urlRootAndPath);
        return this;
    }

    /**
     * <p>Adds a trailing slash (/) to a URL ending with a directory.  A URL is
     * considered to end with a directory if the last path segment,
     * before fragment (#) or query string (?), does not contain a dot,
     * typically representing an extension.</p>
     *
     * <p><b>Please Note:</b> URLs do not always denote a directory structure
     * and many URLs can qualify to this method without truly representing a
     * directory. Adding a trailing slash to these URLs could potentially break
     * its semantic equivalence.</p>
     * <code>http://www.example.com/alice &rarr;
     *       http://www.example.com/alice/</code>
     * @return this instance
     * @deprecated Since 1.11.0, use {@link #addDirectoryTrailingSlash()}
     */
    @Deprecated
    public URLNormalizer addTrailingSlash() {
        return addDirectoryTrailingSlash();
    }

    /**
     * <p>Removes any trailing slash (/) from a URL, before fragment
     * (#) or query string (?).</p>
     *
     * <p><b>Please Note:</b> Removing trailing slashes form URLs
     * could potentially break their semantic equivalence.</p>
     * <code>http://www.example.com/alice/ &rarr;
     *       http://www.example.com/alice</code>
     * @return this instance
     * @since 1.11.0
     */
    public URLNormalizer removeTrailingSlash() {
        String urlRoot = HttpURL.getRoot(url);
        String path = toURL().getPath();
        String urlRootAndPath = urlRoot + path;

        if (path.endsWith("/")) {
            String newPath = StringUtils.removeEnd(path, "/");
            String newUrlRootAndPath = urlRoot + newPath;
            url = StringUtils.replaceOnce(
                    url, urlRootAndPath, newUrlRootAndPath);
        }
        return this;
    }


    /**
     * <p>Removes the unnecessary "." and ".." segments from the URL path.
     * </p>
     * <p><b>As of 2.3.0</b>, the algorithm used to remove the dot segments
     * is the one prescribed by
     * <a href="http://tools.ietf.org/html/rfc3986#section-5.2.4">RFC3986</a>.
     * </p>
     * <code>http://www.example.com/../a/b/../c/./d.html &rarr;
     *       http://www.example.com/a/c/d.html</code>
     * <p><b>Please Note:</b> URLs do not always represent a clean hierarchy
     * structure and the dots/double-dots may have a different signification
     * on some sites.  Removing them from a URL could potentially break
     * its semantic equivalence.</p>
     * @return this instance
     * @see URI#normalize()
     */
    public URLNormalizer removeDotSegments() {
        String path = toURL().getPath().trim();

        // (Bulleted comments are from RFC3986, section-5.2.4)

        // 1.  The input buffer is initialized with the now-appended path
        //     components and the output buffer is initialized to the empty
        //     string.
        StringBuilder in = new StringBuilder(path);
        StringBuilder out = new StringBuilder();

        // 2.  While the input buffer is not empty, loop as follows:
        while (in.length() > 0) {

            // A.  If the input buffer begins with a prefix of "../" or "./",
            //     then remove that prefix from the input buffer; otherwise,
            if (startsWith(in, "../")) {
                deleteStart(in, "../");
            } else if (startsWith(in, "./")) {
                deleteStart(in, "./");
            }

            // B.  if the input buffer begins with a prefix of "/./" or "/.",
            //     where "." is a complete path segment, then replace that
            //     prefix with "/" in the input buffer; otherwise,
            else if (startsWith(in, "/./")) {
                replaceStart(in, "/./", "/");
            } else if (equalStrings(in, "/.")) {
                replaceStart(in, "/.", "/");
            }

            // C.  if the input buffer begins with a prefix of "/../" or "/..",
            //     where ".." is a complete path segment, then replace that
            //     prefix with "/" in the input buffer and remove the last
            //     segment and its preceding "/" (if any) from the output
            //     buffer; otherwise,
            else if (startsWith(in, "/../")) {
                replaceStart(in, "/../", "/");
                removeLastSegment(out);
            } else if (equalStrings(in, "/..")) {
                replaceStart(in, "/..", "/");
                removeLastSegment(out);
            }

            // D.  if the input buffer consists only of "." or "..", then remove
            //      that from the input buffer; otherwise,
            else if (equalStrings(in, "..")) {
                deleteStart(in, "..");
            } else if (equalStrings(in, ".")) {
                deleteStart(in, ".");
            }

            // E.  move the first path segment in the input buffer to the end of
            //     the output buffer, including the initial "/" character (if
            //     any) and any subsequent characters up to, but not including,
            //     the next "/" character or the end of the input buffer.
            else {
                int nextSlashIndex = in.indexOf("/", 1);
                if (nextSlashIndex > -1) {
                    out.append(in.substring(0, nextSlashIndex));
                    in.delete(0, nextSlashIndex);
                } else {
                    out.append(in);
                    in.setLength(0);
                }
            }
        }

        // 3.  Finally, the output buffer is returned as the result of
        //     remove_dot_segments.
        url = StringUtils.replaceOnce(url, path, out.toString());
        return this;
    }
    private static boolean equalStrings(StringBuilder b, String str) {
        return b.length() == str.length() && b.indexOf(str) == 0;
    }
    private static boolean startsWith(StringBuilder b, String str) {
        return b.indexOf(str) == 0;
    }
    private void replaceStart(
            StringBuilder b, String toreplace, String replacement) {
        deleteStart(b, toreplace);
        b.insert(0, replacement);
    }
    private void deleteStart(StringBuilder b, String str) {
        b.delete(0, str.length());
    }
    private void removeLastSegment(StringBuilder b) {
        int index = b.lastIndexOf("/");
        if (index == -1) {
            b.setLength(0);
        } else {
            b.setLength(index);
        }
    }

    /**
     * <p>Removes directory index files.  They are often not needed in URLs.</p>
     * <code>http://www.example.com/a/index.html &rarr;
     *       http://www.example.com/a/</code>
     * <p>Index files must be the last URL path segment to be considered.
     * The following are considered index files:</p>
     * <ul>
     *   <li>index.html</li>
     *   <li>index.htm</li>
     *   <li>index.shtml</li>
     *   <li>index.php</li>
     *   <li>default.html</li>
     *   <li>default.htm</li>
     *   <li>home.html</li>
     *   <li>home.htm</li>
     *   <li>index.php5</li>
     *   <li>index.php4</li>
     *   <li>index.php3</li>
     *   <li>index.cgi</li>
     *   <li>placeholder.html</li>
     *   <li>default.asp</li>
     * </ul>
     * <p><b>Please Note:</b> There are no guarantees a URL without its
     * index files will be semantically equivalent, or even be valid.</p>
     * @return this instance
     */
    public URLNormalizer removeDirectoryIndex() {
        String path = toURL().getPath();
        if (PATTERN_PATH_LAST_SEGMENT.matcher(path).matches()) {
            url = StringUtils.replaceOnce(
                    url, path, StringUtils.substringBeforeLast(path, "/") + "/");
        }
        return this;
    }
    /**
     * <p>Removes the URL fragment (from the "#" character until the end).</p>
     * <code>http://www.example.com/bar.html#section1 &rarr;
     *       http://www.example.com/bar.html</code>
     * @return this instance
     */
    public URLNormalizer removeFragment() {
        url = url.replaceFirst("(.*?)(#.*)", "$1");
        return this;
    }
    /**
     * <p>Replaces IP address with domain name.  This is often not
     * reliable due to virtual domain names and can be slow, as it has
     * to access the network.</p>
     * <code>http://208.77.188.166/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer replaceIPWithDomainName() {
        URL u = toURL();
        if (!PATTERN_DOMAIN.matcher(u.getHost()).matches()) {
            try {
                InetAddress addr = InetAddress.getByName(u.getHost());
                String host = addr.getHostName();
                if (!u.getHost().equalsIgnoreCase(host)) {
                    url = url.replaceFirst(u.getHost(), host);
                }
            } catch (UnknownHostException e) {
                logger.debug("Cannot resolve IP to host for :" + u.getHost(), e);
            }
        }
        return this;
    }
    /**
     * <p>Converts <code>https</code> scheme to <code>http</code>.</p>
     * <code>https://www.example.com/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer unsecureScheme() {
        Matcher m = PATTERN_SCHEMA.matcher(url);
        if (m.find()) {
            String schema = m.group(1);
            if ("https".equalsIgnoreCase(schema)) {
                url = m.replaceFirst(StringUtils.stripEnd(schema, "Ss") + "$2");
            }
        }
        return this;
    }
    /**
     * <p>Converts <code>http</code> scheme to <code>https</code>.</p>
     * <code>http://www.example.com/ &rarr; https://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer secureScheme() {
        Matcher m = PATTERN_SCHEMA.matcher(url);
        if (m.find()) {
            String schema = m.group(1);
            if ("http".equalsIgnoreCase(schema)) {
                url = m.replaceFirst(schema + "s$2");
            }
        }
        return this;
    }
    /**
     * <p>Removes duplicate slashes.  Two or more adjacent slash ("/")
     * characters will be converted into one.</p>
     * <code>http://www.example.com/foo//bar.html
     *       &rarr; http://www.example.com/foo/bar.html </code>
     * @return this instance
     */
    public URLNormalizer removeDuplicateSlashes() {
        String urlRoot = HttpURL.getRoot(url);
        String path = toURL().getPath();
        String urlRootAndPath = urlRoot + path;
        String newPath = path.replaceAll("/{2,}", "/");
        String newUrlRootAndPath = urlRoot + newPath;
        url = StringUtils.replaceOnce(url, urlRootAndPath, newUrlRootAndPath);
        return this;
    }
    /**
     * <p>Removes "www." domain name prefix.</p>
     * <code>http://www.example.com/ &rarr; http://example.com/</code>
     * @return this instance
     */
    public URLNormalizer removeWWW() {
        String host = toURL().getHost();
        String newHost = StringUtils.removeStartIgnoreCase(host, "www.");
        url = StringUtils.replaceOnce(url, host, newHost);
        return this;
    }
    /**
     * <p>Adds "www." domain name prefix.</p>
     * <code>http://example.com/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer addWWW() {
        String host = toURL().getHost();
        if (!host.toLowerCase().startsWith("www.")) {
            url = StringUtils.replaceOnce(url, host, "www." + host);
        }
        return this;
    }
    /**
     * <p>Sorts query parameters.</p>
     * <code>http://www.example.com/?z=bb&amp;y=cc&amp;z=aa &rarr;
     *       http://www.example.com/?y=cc&amp;z=bb&amp;z=aa</code>
     * @return this instance
     */
    public URLNormalizer sortQueryParameters() {
        // Does it have query parameters?
        if (!url.contains("?")) {
            return this;
        }
        // It does, so proceed
        List<String> keyValues = new ArrayList<>();
        String queryString = StringUtils.substringAfter(url, "?");

        // extract and remove any fragments
        String fragment = StringUtils.substringAfter(queryString, "#");
        if (StringUtils.isNotBlank(fragment)) {
            fragment = "#" + fragment;
        }
        queryString = StringUtils.substringBefore(queryString, "#");

        String[] params = StringUtils.split(queryString, '&');
        for (String param : params) {
            keyValues.add(param);
        }
        // sort it so that query string are in order
        Collections.sort(keyValues);

        String sortedQueryString = StringUtils.join(keyValues, '&');
        if (StringUtils.isNotBlank(sortedQueryString)) {
            url = StringUtils.substringBefore(
                    url, "?") + "?" + sortedQueryString + fragment;
        }

        return this;
    }
    /**
     * <p>Removes empty parameters.</p>
     * <code>http://www.example.com/display?a=b&amp;a=&amp;c=d&amp;e=&amp;f=g
     * &rarr; http://www.example.com/display?a=b&amp;c=d&amp;f=g</code>
     * @return this instance
     */
    public URLNormalizer removeEmptyParameters() {
        // Does it have query parameters?
        if (!url.contains("?")) {
            return this;
        }
        // It does, so proceed
        List<String> keyValues = new ArrayList<>();
        String queryString = StringUtils.substringAfter(url, "?");
        String[] params = StringUtils.split(queryString, '&');
        for (String param : params) {
            if (param.contains("=")
                    && StringUtils.isNotBlank(
                    StringUtils.substringAfter(param, "="))
                    && StringUtils.isNotBlank(
                    StringUtils.substringBefore(param, "="))) {
                keyValues.add(param);
            }
        }
        String cleanQueryString = StringUtils.join(keyValues, '&');
        if (StringUtils.isNotBlank(cleanQueryString)) {
            url = StringUtils.substringBefore(
                    url, "?") + "?" + cleanQueryString;
        }
        return this;
    }
    /**
     * <p>Removes trailing question mark ("?").</p>
     * <code>http://www.example.com/display? &rarr;
     *       http://www.example.com/display </code>
     * @return this instance
     */
    public URLNormalizer removeTrailingQuestionMark() {
        if (url.endsWith("?") && StringUtils.countMatches(url, "?") == 1) {
            url = StringUtils.removeEnd(url, "?");
        }
        return this;
    }
    /**
     * <p>Removes a URL-based session id.  It removes PHP (PHPSESSID),
     * ASP (ASPSESSIONID), and Java EE (jsessionid) session ids.</p>
     * <code>http://www.example.com/servlet;jsessionid=1E6FEC0D14D044541DD84D2D013D29ED?a=b
     * &rarr; http://www.example.com/servlet?a=b</code>
     * <p><b>Please Note:</b> Removing session IDs from URLs is often
     * a good way to have the URL return an error once invoked.</p>
     * @return this instance
     */
    public URLNormalizer removeSessionIds() {
        if (StringUtils.containsIgnoreCase(url, ";jsessionid=")) {
            url = url.replaceFirst(
                    "(;jsessionid=([A-F0-9]+)((\\.\\w+)*))", "");
        } else {
            String u = StringUtils.substringBefore(url, "?");
            String q = StringUtils.substringAfter(url, "?");
            if (StringUtils.containsIgnoreCase(url, "PHPSESSID=")) {
                q = q.replaceFirst("(&|^)(PHPSESSID=[0-9a-zA-Z]*)", "");
            } else if (StringUtils.containsIgnoreCase(url, "ASPSESSIONID")) {
                q = q.replaceFirst(
                        "(&|^)(ASPSESSIONID[a-zA-Z]{8}=[a-zA-Z]*)", "");
            }
            if (!StringUtils.isBlank(q)) {
                u += "?" + StringUtils.removeStart(q, "&");
            }
            url = u;
        }
        return this;
    }

    /**
     * <p>Removes trailing hash character ("#").</p>
     * <code>http://www.example.com/path# &rarr;
     *       http://www.example.com/path</code>
     * <p>
     * This only removes the hash character if it is the last character.
     * To remove an entire URL fragment, use {@link #removeFragment()}.
     * </p>
     * @return this instance
     * @since 1.13.0
     */
    public URLNormalizer removeTrailingHash() {
        if (url.endsWith("#") && StringUtils.countMatches(url, "#") == 1) {
            url = StringUtils.removeEnd(url, "#");
        }
        return this;
    }

    /**
     * Returns the normalized URL as string.
     * @return URL
     */
    @Override
    public String toString() {
        return url;
    }
    /**
     * Returns the normalized URL as {@link URI}.
     * @return URI
     */
    public URI toURI() {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        return HttpURL.toURI(url);
    }
    /**
     * Returns the normalized URL as {@link URL}.
     * @return URI
     */
    public URL toURL() {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            logger.info("URL does not appear to be valid and cannot be parsed:"
                    + url, e);
            return null;
        }
    }

    private boolean isEncodedUnreservedCharacter(String enc) {
        // is ALPHA (a-zA-Z)
        if ((enc.compareTo("%41") >= 0 && enc.compareTo("%5A") <= 0)
                || (enc.compareTo("%61") >= 0 && enc.compareTo("%7A") <= 0)) {
            return true;
        }
        // is Digit (0-9)
        if (enc.compareTo("%30") >= 0 && enc.compareTo("%39") <= 0) {
            return true;
        }
        // is hyphen, period, underscore, tilde
        return equalsAny(enc, "%2D", "%2E", "%5F", "%7E");
    }

    private static boolean equalsAny(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (Objects.equals(source, object)) {
                return true;
            }
        }
        return false;
    }

}
