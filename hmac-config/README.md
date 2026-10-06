# HMAC webhook configuration

`hmac.yml` configures verification of HMAC-SHA-256 signatures over the exact
received body bytes. Profiles let different webhook routes use different
signature headers, encodings and secret references. Profile names and secret
selector keys are user-defined; the fields inside a profile are fixed.

The shipped [hmac.yml](src/main/resources/config/hmac.yml) stays disabled by
default and contains a complete commented profiles-map example with GitHub and
shared-secret entries. Copy the example value without an outer wrapper.
The [profiles JSON example](src/main/resources/config/examples/hmac-profiles.json)
contains the same two profiles as a copyable object. These names are examples,
not built-in provider identifiers. Providers that sign timestamps, paths or
other data in addition to the raw body are not supported by this contract.

## Supply the value

For a direct `hmac.yml`, replace the `profiles` placeholder with a YAML map
and set `enabled: true`. For externalized `values.yml` configuration, use:

```yaml
hmac.enabled: true
hmac.profiles:
  github:
    signatureHeader: X-Hub-Signature-256
    signaturePrefix: "sha256="
    secrets:
      defaultEnvNames: [GITHUB_WEBHOOK_SECRET]
```

This minimal example uses one shared secret, the default POST/hex/16 MiB
settings, and disabled replay suppression. Provision a nonempty
`GITHUB_WEBHOOK_SECRET` environment variable in the application process.

For Config Server, define two properties: `hmac.enabled` (boolean) and
`hmac.profiles` (map). The `hmac.profiles` value is the named-profile object in
the JSON example, **without** an outer `profiles` or `hmac.profiles` wrapper.
The Java loader accepts either a map or a JSON object string. Nested profile
fields are not independently externalized configuration properties.

## Field reference

Every supplied profile is validated, even when HMAC is disabled. Enabling HMAC
requires at least one profile. Omitted optional fields use the following
defaults. The schema expects the declared JSON type when a field is supplied;
the runtime also normalizes null/empty allowedMethods to POST. Runtime validation
remains authoritative for conditional secret/replay requirements, case-normalized
method duplicates and provisioned environment variables. The table below is
checked against generated schema descriptions by HmacExamplesTest.

<!-- profile-fields:start -->
| Profile field | Requirement, default and behavior |
| --- | --- |
| `signedInput` | Optional; defaults to rawBody, the only supported signed input. Verify the exact received body bytes without parsing or normalization. |
| `algorithm` | Optional; defaults to hmacSha256, the only supported algorithm. |
| `allowedMethods` | Optional; omitted, null or empty defaults to [POST]. Unique subset of POST, PUT and PATCH; method names are normalized to uppercase. |
| `signatureHeader` | Required nonblank HTTP header name carrying the signature. GitHub uses X-Hub-Signature-256. Header names are case-insensitive. |
| `signaturePrefix` | Optional; defaults to an empty string. Exact case-sensitive prefix removed before decoding; GitHub uses sha256=. Characters below U+0020 are forbidden; DEL and C1 characters are not rejected by the current runtime. |
| `signatureEncoding` | Optional; defaults to hex. Supports hex or base64, normalized to lowercase. |
| `maxBodyBytes` | Optional positive byte limit; defaults to 16777216 (16 MiB). Request injection must have enough exact-body buffering capacity. |
| `secrets.selectorHeader` | Optional; defaults to an empty string. Required when bySelector is non-empty. Selects a secret list using an exact header value after trimming HTTP whitespace. |
| `secrets.bySelector` | Optional map; defaults to empty. Each selector maps to one or two distinct nonblank environment-variable names, current first and previous second. Quote numeric selector keys in YAML. |
| `secrets.defaultEnvNames` | Optional; defaults to an empty list (no fallback). One or two distinct nonblank environment-variable names for a shared secret or an explicit fallback for missing/unknown selectors. At least this list or bySelector must be non-empty. |
| `replay.enabled` | Optional; defaults to false. Enable delivery-ID replay suppression; requires idHeader and a configured WebhookReplayStore singleton. |
| `replay.idHeader` | Optional when replay is disabled; required nonblank HTTP header name when enabled. GitHub uses X-GitHub-Delivery. |
| `replay.retentionSeconds` | Optional positive retention time in seconds; defaults to 604800 (seven days). |
<!-- profile-fields:end -->

At least `secrets.bySelector` or `secrets.defaultEnvNames` must be nonempty.
Order secret references current first, previous second for rotation. Every
referenced variable must be provisioned and nonempty when the runtime loads.
Secret values are never placed in the config, Config Server, or snapshots.

With a selector header, values match exactly after trimming HTTP whitespace;
quote numeric selector keys in YAML. Unknown or missing selector values use
`defaultEnvNames` only when an explicit fallback is configured. An empty list
disables fallback. Header names are case-insensitive; ambiguous multiple
signature, selector or delivery-ID header values are rejected.

## Wire the handler and route

The profiles map alone does not install the handler. Include the `hmac` artifact
and configure the application chain:

1. `RequestInterceptorInjectionHandler` buffers the raw body. Configure
   `request-injection.appliedBodyInjectionPathPrefixes` to cover the webhook
   paths, enable request injection, and set a positive exact `maxBodyBytes`
   at least as large as the profile limit. `maxBuffers × server.bufferSize` must
   be at least `request-injection.maxBodyBytes`. Startup rejects any enabled
   request-transformer `appliedPathPrefixes` entry overlapping an HMAC prefix.
   Do not mutate the body before verification.
2. Put `HmacRequestInterceptor` first among `RequestInterceptor` singleton
   implementations in `service.yml`.
3. Put `HmacHandler` immediately before `UnifiedSecurityHandler` in the active
   `handler.yml` chain.
4. Reference the profile in `unified-security.yml`:

   ```yaml
   pathPrefixAuths:
     - prefix: /github-webhook
       hmacProfile: github
   ```

   Java uses ordered first-prefix matching. Avoid shadowing the HMAC route or
   overlapping `anonymousPrefixes`. HMAC can be used alone, with JWT, or with API key. HMAC plus both JWT
   and API key is rejected, as are combinations with Basic, SJWT or SWT.

For replay-enabled profiles, copy the replay-store binding from
[hmac-service.yml](../hmac/src/main/resources/config/hmac-service.yml) into the
application's actual `service.yml`. The local store is process-local and loses
reservations on restart. Multi-instance suppression needs a distributed
implementation such as the optional `hmac-redis` adapter. Replay storage is
selected through `service.yml`, not a field inside the profile.

Duplicates return an empty `200` without calling the application. Non-2xx
completion releases the reservation for retry. GitHub's delivery ID is not
included in its body signature: this is delivery deduplication, not a complete
cryptographic anti-replay guarantee. Downstream idempotency remains necessary.

Configuration reload can change references to already provisioned environment
variables. Changing their secret bytes or the `service.yml` provider binding
requires a process restart. The previous-secret variable is optional, but if
its name is listed, the variable must exist.

See the [Java HMAC guide](https://github.com/networknt/networknt-doc/blob/master/src/concern/middleware/hmac-webhook-authentication.md)
for complete chain wiring, distributed replay configuration and operations.
