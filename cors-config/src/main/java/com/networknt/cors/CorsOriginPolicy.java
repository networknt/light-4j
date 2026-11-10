/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
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
 */

package com.networknt.cors;

import com.google.common.net.InetAddresses;
import com.google.common.net.HostAndPort;
import com.google.common.net.InternetDomainName;
import com.networknt.config.ConfigException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** An immutable, prevalidated origin allowlist. No DNS or other network calls are made. */
public final class CorsOriginPolicy {
    private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");
    private static final Pattern HOST_LABEL = Pattern.compile("[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?");
    private static final Pattern NUMERIC_HOST = Pattern.compile("[0-9.]+");
    private final List<Rule> rules;

    private CorsOriginPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static CorsOriginPolicy compile(Collection<?> origins, String location) {
        List<Rule> rules = new ArrayList<>();
        if (origins != null) {
            int index = 0;
            for (Object value : origins) rules.add(compileRule(value, location + "[" + index++ + "]"));
        }
        return new CorsOriginPolicy(rules);
    }

    /** Exact-only compatibility lookup: returns the configured entry, never enables patterns. */
    static String matchLegacy(String value, Collection<?> origins, Supplier<String> serverOrigin) {
        final String comparable;
        if ("null".equals(value)) {
            comparable = value;
        } else {
            try {
                comparable = sanitizeDefaultPort(value, parse(value));
            } catch (IllegalArgumentException _) {
                return null;
            }
        }
        if (origins != null) {
            for (Object item : origins) {
                if (item instanceof String origin && origin.indexOf('*') < 0 && origin.equalsIgnoreCase(comparable)) return origin;
            }
        }
        String fallback = serverOrigin.get();
        return fallback != null && fallback.equalsIgnoreCase(comparable) ? fallback : null;
    }

    private static Rule compileRule(Object value, String location) {
        try {
            if (!(value instanceof String text)) throw new IllegalArgumentException("expected a non-null string");
            if ("null".equals(text)) return new Rule(null, false);
            boolean wildcard = text.indexOf('*') >= 0;
            String candidate = text;
            if (wildcard) {
                int schemeEnd = text.indexOf("://");
                if (schemeEnd < 0 || !text.startsWith("*.", schemeEnd + 3)
                        || text.indexOf('*') != text.lastIndexOf('*')) {
                    throw new IllegalArgumentException("wildcard must be the entire leftmost hostname label");
                }
                candidate = text.substring(0, schemeEnd + 3) + text.substring(schemeEnd + 5);
            }
            Origin origin = parse(candidate);
            if (wildcard && (!isHttp(origin.scheme) || !origin.dnsHost
                    || !InternetDomainName.from(origin.host).isUnderPublicSuffix())) {
                // Includes PRIVATE rules, PSL wildcard/exception rules, and the fixed-label constraint.
                throw new IllegalArgumentException("HTTP/HTTPS wildcard suffix must be below a known public/private DNS suffix");
            }
            return new Rule(origin, wildcard);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(location + ": invalid origin '" + value + "': " + e.getMessage());
        }
    }

    public boolean matches(String value) {
        return matches(value, () -> null);
    }

    /** Parse the request once; compute and parse a server fallback only after a policy miss. */
    boolean matches(String value, Supplier<String> serverOrigin) {
        if ("null".equals(value)) return rules.stream().anyMatch(rule -> rule.origin == null);
        final Origin request;
        try {
            request = parse(value);
        } catch (IllegalArgumentException _) {
            return false;
        }
        for (Rule rule : rules) {
            if (rule.matches(request)) return true;
        }
        try {
            String fallback = serverOrigin.get();
            return fallback != null && request.equals(parse(fallback));
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    /** Compare exact parsed origins, including legacy internal hosts and non-HTTP schemes. */
    public static boolean sameOrigin(String first, String second) {
        try {
            return parse(first).equals(parse(second));
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    static String sanitizeDefaultPort(String value) {
        try {
            return sanitizeDefaultPort(value, parse(value));
        } catch (IllegalArgumentException _) {
            // This compatibility utility leaves malformed input unchanged; matching rejects it.
            return value;
        }
    }

    private static String sanitizeDefaultPort(String value, Origin origin) {
        int colon = value.lastIndexOf(':');
        if (isHttp(origin.scheme) && colon > value.indexOf("://") + 2 && colon > value.lastIndexOf(']')
                && origin.port == defaultPort(origin.scheme)) return value.substring(0, colon);
        return value;
    }

    private static Origin parse(String value) {
        if (!isAsciiToken(value)) throw new IllegalArgumentException("expected an ASCII origin (use Punycode for IDNs)");
        int separator = value.indexOf("://");
        if (separator < 1 || !SCHEME.matcher(value.substring(0, separator)).matches()) {
            throw new IllegalArgumentException("expected scheme://host[:port]");
        }
        String scheme = value.substring(0, separator).toLowerCase(Locale.ROOT);
        HostPort authority = parseAuthority(value.substring(separator + 3).toLowerCase(Locale.ROOT));
        int port = authority.port == -1 ? defaultPort(scheme) : authority.port;
        return new Origin(scheme, authority.host, port, authority.dnsHost);
    }

    private static boolean isAsciiToken(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) <= 32 || value.charAt(i) >= 127) return false;
        }
        return true;
    }

    private static HostPort parseAuthority(String authority) {
        // Guava accepts empty ports/negative zero; some versions accept scoped IP literals.
        if (authority.endsWith(":") || authority.contains(":-") || authority.indexOf('%') >= 0) throw new IllegalArgumentException("invalid origin authority");
        HostAndPort parsed = HostAndPort.fromString(authority).requireBracketsForIPv6();
        String host = parsed.getHost();
        boolean ipv6 = host.indexOf(':') >= 0;
        if (ipv6) host = "[" + InetAddresses.toAddrString(InetAddresses.forString(host)) + "]";
        boolean dnsHost = !ipv6 && validateHostname(host);
        return new HostPort(host, parsed.getPortOrDefault(-1), dnsHost);
    }

    private static boolean validateHostname(String host) {
        if (host.isEmpty() || host.length() > 253) throw new IllegalArgumentException("invalid hostname length");
        boolean trailingDot = host.endsWith(".");
        String name = trailingDot ? host.substring(0, host.length() - 1) : host;
        for (String label : name.split("\\.", -1)) {
            if (!HOST_LABEL.matcher(label).matches()) throw new IllegalArgumentException("invalid hostname label");
        }
        if (NUMERIC_HOST.matcher(name).matches()) {
            if (!InetAddresses.isInetAddress(name)) throw new IllegalArgumentException("invalid IPv4 literal");
            return false;
        }
        return !trailingDot && host.indexOf('_') < 0;
    }

    private static int defaultPort(String scheme) {
        return switch (scheme) { case "http" -> 80; case "https" -> 443; default -> -1; };
    }

    private static boolean isHttp(String scheme) {
        return scheme.equals("http") || scheme.equals("https");
    }

    private record Origin(String scheme, String host, int port, boolean dnsHost) { }
    private record HostPort(String host, int port, boolean dnsHost) { }
    private record Rule(Origin origin, boolean wildcard) {
        boolean matches(Origin request) {
            return origin != null && origin.scheme.equals(request.scheme) && origin.port == request.port
                    && (wildcard ? request.dnsHost && request.host.endsWith("." + origin.host)
                                 : request.host.equals(origin.host));
        }
    }
}
