# Token exchange


## Native primitive configuration binding

This companion to the client-config workaround uses private primitive setters for six boolean properties in TokenExchangeConfig, RequestSchema and UpdateSchema. Jackson 3.2.3 field-backed MethodProperty binding fails in GraalVM CE 25.0.2 with a Boolean value reaching a field accessor as Integer. Primitive method-backed binding succeeds. RequestSchema and UpdateSchema reproduce the field-only failure; primitive integer binding succeeds, so both proxyPort fields remain unchanged.

The primitive parameters preserve mapper coercion and strict-null policy. UpdateSchema retains its explicit-null skip behavior: null leaves updateExpirationFromTtl enabled under either mapper policy; an empty string becomes false under the default policy and is rejected under strict-null policy. Public getters and property names are unchanged, and no public mutator is added.

The token-exchange JAR owns reflection metadata for these three configuration classes and their private binding methods. After source-installing prerequisites and building the module, run the standalone JVM/native check from the repository root:

```bash
JACKSON3_MAVEN_REPO=/absolute/isolated/m2 JACKSON3_NATIVE_MAVEN_PROFILE=native-config-binding bash scripts/verify-native-config-binding.sh token-exchange NativeTokenExchangeBindingTest
```

The offline script uses the pinned cached GraalVM image, with all charsets available for SnakeYAML. The test uses a temporary synthetic config with no encryption and an empty token-exchange cache entry. It exercises real constructors and module-owned metadata, six properties across 156 coercion/serialization cases, 12 omitted defaults and both proxyPort integer fields. This does not qualify encrypted config, TLS, token providers or a deployed native gateway. CI runs this regression after full-core tests and the client-config native check.

The native-config-binding profile pins WildFly Common 2.0.1 only for this fixture, matching the gateway's native toolchain. The normal token-exchange dependency graph is unchanged. Without the profile, the older transitive WildFly Common substitution prevents native-image analysis before this regression can run. This is a configuration-binding qualification under that profile, not qualification of the ordinary unmodified native dependency graph. Serialization assertions use a test-only writer with FAIL_ON_EMPTY_BEANS disabled because RequestSchema exposes an unrelated empty lock object; production mapper policy is unchanged.
