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

Existing four-part AES-GCM values keep the same PBKDF2-HMAC-SHA256 parameters
(65,536 iterations, 256-bit key) and 128-bit authentication tag. The format is
not a versioned envelope; use the supported producer. Authentication failure
returns no plaintext.

## Credential lifetime and compatibility

Password-provider selection is unchanged. The base `AESSaltDecryptor` retains
its historical public `light` default for compatibility; that default is not a
production secret. Production deployments should use `AutoAESSaltDecryptor`
with an explicitly supplied `light_4j_config_password` (or uppercase equivalent),
or a custom password provider. The existing Auto JUnit-only fallback remains.

Derived keys and Auto's password arrays are no longer cached. A password change
is observed on the next decrypt call; it does not reload already cached/decrypted
application configuration. Repeated decryptions now repeat PBKDF2 work.

PBEKeySpec's password copy, temporary encoded key bytes, decrypted byte buffers,
and fresh Auto/manual password arrays are cleared after use. Custom providers'
arrays remain borrowed by default; a provider that returns a fresh owned array
can override `releasePassword(char[])` to clear it. TLS initialization clears
its owned temporary password copies on success and failure. MRAS no longer logs
password prefixes.

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
