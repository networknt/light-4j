# Four-part CRYPT migration (master and 2.x)

Both branches now accept only authenticated AES-GCM values:

```text
CRYPT:<salt-hex>:<nonce-hex>:<ciphertext-and-tag-hex>
```

Three-part `CRYPT:<salt-hex>:<ciphertext-hex>` values used legacy zero-IV
AES-CBC. They are rejected before password retrieval. There is no compatibility
switch. Applications with these values must re-encrypt before upgrading.

1. Inventory CRYPT values in every configuration source, including external
   secret stores, environment overrides, and configuration-server snapshots.
   Count fields without printing the secrets or decrypted values.
2. Obtain the original plaintext from your authorized secret source. If it is
   only available in legacy ciphertext, recover it using the previous release
   in a controlled offline environment before upgrading. Do not add a legacy
   decryption fallback to the upgraded service.
3. Re-encrypt each secret with the current **light-encryptor** AES-GCM producer
   and your intended master password. It generates a fresh random 16-byte salt
   and 12-byte nonce for each encryption. Do not append a field to existing CBC
   ciphertext or reuse a fixed nonce. Avoid putting plaintext or the master
   password into shared logs, shell history, or exposed process arguments.
4. Replace the deployed values and verify startup and affected TLS/authentication
   flows in a controlled environment before rolling out the new release.
   Retain a protected rollback configuration suitable for the previous release.

Standard four-part AES-GCM values require exactly a 16-byte salt, a 12-byte
nonce and at least a 16-byte authentication tag. These lengths and hex fields
are validated before password acquisition or PBKDF2. Nonstandard four-part
values must also be re-encrypted with the supported producer. Standard values
keep the same PBKDF2-HMAC-SHA256 parameters
(65,536 iterations, 256-bit key) and 128-bit authentication tag. The format is
not a versioned envelope; use the supported producer. Authentication failure
returns no plaintext.

The configuration loader reports the configuration filename; YAML decrypt
errors additionally report the line and column without formatting source marks
or echoing the secret. The standalone decryptor receives only a String and
cannot identify a configuration key/file on its own.

## Credential lifetime and compatibility

**Breaking change on master and 2.x:** the base `AESSaltDecryptor` no longer
supplies the public `light` password. It remains concrete for existing service.yml
bindings, but supplies no password and throws when password acquisition is
requested. This withdraws the previous
compatibility waiver; see [issue #2831](https://github.com/networknt/light-4j/issues/2831).

Select `AutoAESSaltDecryptor` with an explicitly supplied
`light_4j_config_password` (or uppercase equivalent), the interactive
`ManualAESSaltDescryptor`, or a custom password provider. Auto has no JUnit
detection or public-password fallback. Tests supply synthetic passwords in scoped
test setup that restores the original property afterward. No password is supplied
by the parent POM.
Custom providers keep their existing ownership contract.

Inventory deployed `decryptorClass` settings, service.yml Decryptor bindings,
direct instantiations, and subclasses calling `super.getPassword()` before
upgrading. If the base class was using its public default, decrypt and re-encrypt
affected values in a controlled offline migration with light-encryptor and a deployment-specific master secret.
Do not print plaintext or put real secrets in commands, logs or issues. Replace
the provider selection and distribute the new runtime secret before adopting the
updated library. Supplying the old public password explicitly is not remediation.

The base and Auto providers do not cache derived keys or master passwords. A password change
is observed on the next decrypt call; it does not reload already cached/decrypted
application configuration. Repeated decryptions through these providers repeat PBKDF2 work.

PBEKeySpec's password copy, temporary encoded key bytes, decrypted byte buffers,
and fresh Auto/manual password arrays are cleared after use. Custom providers'
arrays remain borrowed by default; a provider that returns a fresh owned array
can return `ownedPassword(freshArray)` to register it for operation-scoped
cleanup. The inherited Auto/manual cleanup never infers ownership of an array
returned by a downstream `getPassword()` override. TLS initialization clears
its owned temporary password copies on success and failure. MRAS no longer logs
password prefixes.

### Interactive manual provider

`ManualAESSaltDescryptor` caches at most 32 successfully authenticated derived
keys per instance, with a five-minute reuse window, so repeat decrypts of the
same salt do not prompt for the master password on every call. It does not
retain the master password. Failed GCM authentication does not populate the
cache. Uncached salts prompt, and prompts return after expiry or capacity
eviction. Expiration is checked lazily on the next key acquisition; it is not
a background memory-erasure timer. Oldest-key eviction clears the cache's owned
key bytes. Provider-internal key copies remain outside this guarantee.

The manual provider implements `AutoCloseable`: close it when its configuration
session ends to clear all cached key buffers and reject further decryptions.
Use a new instance after a master-password change. Applications selecting this
provider through framework configuration must arrange that lifecycle cleanup;
otherwise idle entries remain bounded but are retained until the next lookup
or instance reclamation. Auto/base providers do not use this interactive cache.

These are scoped A20 hardening changes, not complete heap erasure. Immutable
configuration/password Strings, returned plaintext Strings, authentication
headers, provider-internal keys, and credentials retained by configured clients
still exist. TlsConfig's retained arrays and authentication APIs are unchanged;
clearing their borrowed arrays would break subsequent use. Heap dumps and
configuration stores still require access controls. A20 findings in other
repositories are outside this change.

A17's password-as-salt interpretation is not confirmed: the decryptor reads the
salt from ciphertext and the current producer uses random salts. This change
does not establish how historical deployed values were generated. A18's legacy
CBC path is removed; a remote padding oracle was not demonstrated.

Tracked by [light-4j #2828](https://github.com/networknt/light-4j/issues/2828).

### Derivation-cost observation

A local Java 25.0.3 probe, after 20 warm-up decrypts, measured 100 repeated
base-provider decryptions of one synthetic GCM value at about 1,069 ms total
(10.69 ms/value), versus 7.3 ms total (0.073 ms/value) with the pre-change warm
key cache. This is an informational single-process observation, not a capacity
benchmark. It measures the repeated-value case; the old cache also had to derive
each previously unseen salt. Base/Auto deliberately pay this repeated PBKDF2
cost to avoid retaining password-derived keys and to observe password changes.
The interactive manual provider uses the bounded lifecycle described above.

## Test-suite migration

Auto no longer detects JUnit and its public `isJUnitTest()` helper was removed.
Remove calls to that helper. Set the synthetic password in the test case before
loading encrypted fixtures and restore the previous property afterward. Do not
set it in production resources or the parent POM. Other repositories, including
light-rest-4j, must migrate their encrypted test fixtures as part of their upgrade.
Configuration loading rejects a bare-base `decryptorClass` selection with provider
guidance. Use Auto, Manual, or a custom provider instead. A bare-base service.yml
binding still constructs and allows later singleton registrations; it throws
when a valid CRYPT value first requests a password. Service-factory behavior is
unchanged. This fix does not promise startup-time validation for all provider
bindings or direct instances.

For JUnit 6.1+, use `org.junit.jupiter.api.util.SetSystemProperty` on the fixture
test class or method. It sets the explicit test password and restores the original
property automatically, including IDE runs. Initialize encrypted configuration in
`@BeforeAll` or `@BeforeEach`, after the annotation has taken effect, rather than
static fields. Class-level annotations also cover `@BeforeAll`.
No production provider distinguishes JUnit from other callers.
