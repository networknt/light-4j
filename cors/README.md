# CORS origins and scoped wildcards

`allowedOrigins` and each `pathPrefixAllowed` entry accept explicit origins and
scoped patterns:

```yaml
cors.allowedOrigins:
  - https://*.example.com
  - https://example.com
  - https://*.example.org:8443
```

The first pattern accepts `https://app.example.com` and
`https://team.app.example.com`. It excludes the apex, `badexample.com`, and
`example.com.attacker.net`. List the apex separately. The wildcard must occupy
the entire leftmost hostname label. Unrestricted `*`, partial-label wildcards,
wildcard IP addresses, and patterns elsewhere in the hostname are invalid.

Wildcard patterns accept HTTP/HTTPS schemes and strict DNS hostnames without
underscores or trailing dots. Exact entries preserve custom schemes such as
`capacitor://localhost`, `ionic://localhost`, `chrome-extension://<id>`, and
`tauri://localhost`, as well as internal hostnames containing underscores and
hosts with a trailing dot. Those exact-host forms also work with the legacy
server-origin fallback. A trailing dot remains significant for exact matching;
it is not removed and never matches a wildcard. Underscores never match a
wildcard either.

Schemes and hosts are compared without case sensitivity; absent ports normalize
to 80 for HTTP and 443 for HTTPS. Custom schemes have no implicit default port.
Other ports must match exactly (valid range: 0–65535). Hostnames use ASCII;
configure internationalized names using their ASCII Punycode form. Unicode input
is rejected. Explicit localhost, IPv4 and bracketed IPv6 origins remain supported.
IPv6 literals normalize to their canonical address form.
Userinfo, paths (including `/`), queries, fragments, malformed ports/hosts,
multiple Origin headers, and origin lists are rejected. The opaque origin
`null` never matches a wildcard; an explicit string `"null"` entry retains
legacy opt-in support and should be used only with a deliberate trust decision.

Accepted responses contain exactly one `Access-Control-Allow-Origin` value:
the validated request origin, with `Access-Control-Allow-Credentials: true`.
A response-commit listener merges `Origin` into `Vary` after downstream/proxy
headers have been copied, preserving backend cache keys on CORS and non-CORS
responses, including denied and no-Origin requests. An existing `Vary: *` already
covers variation; an existing case-insensitive Origin token is not duplicated.
Rejected actual and preflight requests return 403 and do not invoke downstream
handlers. Requests without an Origin header continue downstream normally, even
when they carry Access-Control-Request-Method or Access-Control-Request-Headers.
OPTIONS requests without Origin are not treated as CORS preflights.

## Trust boundary

Every matching subdomain, at every depth, is trusted to read credentialed
responses. Use patterns only for trusted, organization-controlled subdomains.
Delegated, tenant-controlled, compromised, or abandoned subdomains may expose
authenticated responses. A PSL check does not establish organizational ownership
or trust. CORS does not replace authentication, authorization, or CSRF protection.

## Compatibility and path selection

This feature preserves the existing server-origin fallback: after an allowlist
miss, an origin matching the exchange's scheme, hostname, and port is accepted.
This also applies when the selected origins list is missing or empty. Such lists
therefore do not express an absolute deny-all policy. Configure virtual hosts
and any forwarded-header handlers to trust only the appropriate proxies; the
fallback uses the exchange metadata, not a configured canonical public origin.

Per-path selection preserves the first raw string prefix match in map iteration
order. A selected entry replaces global origins **and** methods. Missing values
do not inherit the global values; the server-origin fallback still applies.
Unmatched paths use the global policy. Prefix matching is not segment-aware:
`/api` matches `/api-other`, and an earlier `/api` shadows `/api/admin`. Put more
specific entries first and ensure the configuration source preserves their order.
No longest-prefix or segment-boundary migration is introduced by this feature.

```yaml
cors.pathPrefixAllowed:
  /api/admin:
    allowedOrigins:
      - https://admin.example.com
    allowedMethods: [GET]
  /api/:
    allowedOrigins:
      - https://*.example.com
    allowedMethods: [GET, PUT]
```

Strict request-origin parsing is an intentional hardening change: malformed or
multiple Origin values are rejected, while the exact-origin compatibility forms
above are retained. Rejected preflights
now consistently return 403 rather than subsequently being overwritten with 200.

## PSL and configuration lifecycle

Wildcard suffixes must have at least two fixed DNS labels and be below a known
Public Suffix List suffix. `*.com`, `*.co.uk`, `*.github.io`, `*.blogspot.com`,
and unknown/internal suffix patterns such as `*.example.internal` are rejected.
Explicit internal origins are supported. PSL wildcard and exception rules are
honored, including private suffix rules.

The PSL is bundled in Guava **33.8.0-jre**, managed through `version.guava` and dependency management in the parent `pom.xml`.
Only `cors-config` declares the compile dependency; consumers receive it
transitively because runtime policy validation uses its PSL data.
There are no startup or request-time PSL downloads or DNS lookups. Framework
maintainers own updates: review Guava/PSL changes during dependency maintenance,
upgrade the pin for relevant suffix changes, rerun the CORS tests, and ship the
updated artifact. The bundled list can lag the live PSL; updating a dependency
does not establish that subdomains are trusted. Downstream dependency overrides
must preserve the tested PSL behavior.

All global and per-path patterns compile before enabled configuration activation,
even for paths that have not received traffic. Disabled CORS does not validate
unused origin patterns or methods, and its handler passes through without adding
headers. Enabling it first validates the complete policy; invalid values leave it
disabled. Invalid initial enabled configuration fails with the offending
property/index/pattern. Empty items in comma-separated externalized lists are
ignored; null or non-string list elements report actionable configuration errors. Reload follows the configuration cache's
map-replacement contract: invalid refreshes log an error once per replacement
map and retain the entire last valid policy. A later valid replacement activates
atomically. Correct a failed revocation promptly: the previous policy remains in
force until a valid replacement is applied. Named CORS configurations use the
same validation and caching behavior. The public collection-based `matchOrigin`
compatibility helper keeps one compiled collection snapshot per handler, rebuilds
it when collection contents change, and treats malformed legacy entries as
non-matches. Its rejection path retains the 403/audit contract. The public
`CorsUtil.sanitizeDefaultPort` utility remains as a deprecated adapter to the
same parser rather than exposing a second port-parsing implementation.

## Validation

Run matcher, configuration-lifecycle, and real Undertow handler tests:

```sh
mvn -pl cors-config,cors -am test -Dtest='Cors*Test' -Dsurefire.failIfNoSpecifiedTests=false
```

An opt-in Chromium smoke test verifies credentialed GET and preflighted PUT from
single- and multi-level subdomains, excludes apex/boundary lookalikes, and checks
the fixture requires a cookie. It uses an ephemeral loopback server and Chromium
hostname mappings; it does not access real example.com services. Install Node,
Playwright and its Chromium browser in a local tools directory, then run:

```sh
CORS_PLAYWRIGHT_MODULE=/absolute/path/to/node_modules/playwright \
  mvn -pl cors-config,cors -am test -Dtest=CorsBrowserSmokeTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dcors.browserSmoke=true
```

The browser test is disabled in ordinary Maven runs; server-side tests run
without Node or browser dependencies. To use an existing compatible Chromium
installation, set `CORS_CHROMIUM_EXECUTABLE` to its executable path. Upstream references:
[Fetch Origin syntax](https://fetch.spec.whatwg.org/#origin-header),
[CORS checks](https://fetch.spec.whatwg.org/#cors-check),
[cache variation](https://fetch.spec.whatwg.org/#cors-protocol-and-http-caches),
and [PSL maintenance](https://publicsuffix.org/list/).
