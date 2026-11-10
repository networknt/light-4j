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
import com.google.common.net.InternetDomainName;
import com.networknt.config.ConfigException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** An immutable, prevalidated origin allowlist. No DNS or other network calls are made. */
public final class CorsOriginPolicy {
    private final List<Rule> rules;

    private CorsOriginPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static CorsOriginPolicy compile(Collection<String> origins, String location) {
        List<Rule> rules = new ArrayList<>();
        if (origins != null) {
            for (String value : origins) {
                try {
                    if ("null".equals(value)) {
                        rules.add(new Rule(null, false));
                        continue;
                    }
                    boolean wildcard = value != null && value.contains("*");
                    String candidate = value;
                    if (wildcard) {
                        int schemeEnd = value.indexOf("://");
                        if (schemeEnd < 0 || !value.startsWith("*.", schemeEnd + 3)
                                || value.indexOf('*') != value.lastIndexOf('*')) {
                            throw new IllegalArgumentException("wildcard must be the entire leftmost hostname label");
                        }
                        candidate = value.substring(0, schemeEnd + 3) + value.substring(schemeEnd + 5);
                    }
                    Origin origin = parse(candidate);
                    if (wildcard) {
                        if (origin.host.startsWith("[") || InetAddresses.isInetAddress(origin.host)
                                || origin.host.split("\\.").length < 2) {
                            throw new IllegalArgumentException("wildcard requires at least two fixed DNS labels");
                        }
                        // isUnderPublicSuffix includes PRIVATE rules and PSL wildcard/exception rules.
                        if (!InternetDomainName.from(origin.host).isUnderPublicSuffix()) {
                            throw new IllegalArgumentException("wildcard suffix must be below a known public/private suffix");
                        }
                    }
                    rules.add(new Rule(origin, wildcard));
                } catch (IllegalArgumentException e) {
                    throw new ConfigException(location + ": invalid origin '" + value + "': " + e.getMessage());
                }
            }
        }
        return new CorsOriginPolicy(rules);
    }

    public boolean matches(String value) {
        if ("null".equals(value)) {
            return rules.stream().anyMatch(rule -> rule.origin == null);
        }
        final Origin request;
        try {
            request = parse(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (Rule rule : rules) {
            Origin allowed = rule.origin;
            if (allowed != null && allowed.scheme.equals(request.scheme) && allowed.port == request.port
                    && (rule.wildcard ? request.host.endsWith("." + allowed.host) : request.host.equals(allowed.host))) {
                return true;
            }
        }
        return false;
    }

    /** Compare parsed origins for the legacy server-origin fallback without compiling configuration. */
    public static boolean sameOrigin(String first, String second) {
        try {
            return parse(first).equals(parse(second));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Origin parse(String value) {
        if (value == null || value.isEmpty() || value.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalArgumentException("expected an ASCII HTTP/HTTPS origin (use Punycode for IDNs)");
        }
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("malformed origin", e);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || host == null || uri.getRawUserInfo() != null || !uri.getRawPath().isEmpty()
                || uri.getRawQuery() != null || uri.getRawFragment() != null || host.endsWith(".")) {
            throw new IllegalArgumentException("expected scheme://host[:port], without trailing dot, userinfo, path, query or fragment");
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("[")) {
            if (!InetAddresses.isInetAddress(host.substring(1, host.length() - 1))) {
                throw new IllegalArgumentException("invalid IPv6 literal");
            }
            host = "[" + InetAddresses.toAddrString(InetAddresses.forString(host.substring(1, host.length() - 1))) + "]";
        } else {
            if (host.length() > 253) throw new IllegalArgumentException("hostname too long");
            for (String label : host.split("\\.", -1)) {
                if (label.length() > 63 || !label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) {
                    throw new IllegalArgumentException("invalid DNS label");
                }
            }
            if (host.matches("[0-9.]+") && !InetAddresses.isInetAddress(host)) {
                throw new IllegalArgumentException("invalid IPv4 literal");
            }
        }
        String authority = uri.getRawAuthority();
        int port = uri.getPort();
        if (authority.endsWith(":") || port < -1 || port > 65535) {
            throw new IllegalArgumentException("invalid port (expected 0 through 65535)");
        }
        if (port == -1) port = scheme.equals("https") ? 443 : 80;
        return new Origin(scheme, host, port);
    }

    private record Origin(String scheme, String host, int port) { }
    private record Rule(Origin origin, boolean wildcard) { }
}
