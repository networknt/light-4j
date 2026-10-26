## client-config

The client-config module is a component shared by both the client module in light-4j and light-aws-lambda for both the Undertow-based client and JDK 11 http-client. It is moved from the client module with the new features to support reloading.

It is based on the same client.yml and supports Undertow and JDK  client syntaxes. To ensure that this module is backward-compatible, we will follow the Undertow client syntax if there are any discrepancies.


## Jackson 3 native boolean binding

With Jackson 3.2.3 and GraalVM CE 25.0.2, a private annotated primitive boolean field can fail during `MethodProperty.deserializeAndSet`: Jackson reports a Boolean value, but the field accessor receives Integer and throws `Can not set boolean field ... to java.lang.Integer`. Jackson 3 routes annotated fields through `MethodHandles.lookup().unreflectSetter(...).asType(...)`. An otherwise equivalent annotated primitive setter succeeds. A small standalone field-only bean reproduces the failure; a direct JDK field-handle probe succeeds, so this identifies the Jackson/native-image interaction rather than establishing an upstream compiler defect.

OAuthSignConfig uses a **private primitive setter** to select the working method path without adding a public mutator. Keeping the argument primitive preserves mapper coercion and `FAIL_ON_NULL_FOR_PRIMITIVES`: omitted input retains true, ordinary explicit null becomes false, and strict explicit null is rejected. Empty string follows the same null policy; string `"1"` and numeric `0.0` are rejected. Serialization still emits `enableHttp2`.

The client-config JAR carries its own reflection metadata for this class, including the private setter. Consumers do not need to copy gateway public-method registrations for this binding. This qualifies OAuthSignConfig, not every configuration graph or native application.

After source-installing prerequisites into an isolated Maven cache and building client-config, run:

```bash
JACKSON3_MAVEN_REPO=/absolute/isolated/m2 bash scripts/verify-native-config-binding.sh
```

The script runs offline, builds a native executable from the module JAR plus its runtime dependencies, and supplies no application reflection config. It uses a pinned GraalVM image already present locally; CI pulls that digest explicitly. The executable checks 26 coercion/serialization cases under both null policies plus omitted defaults. CI runs it after the full core suite. An isolated no-setter mutation fails this test with the original error.

The same field-only failure was reproduced in token-exchange RequestSchema and UpdateSchema; their qualification and any additional workaround belong to a separate token-exchange change. TokenExchangeConfig requires its own initialization-aware probe. No token-exchange production code is changed by this fix.
