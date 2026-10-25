# Shared Jackson compatibility policy

`com.networknt:jackson-defaults` depends only on Jackson databind and its core/annotation dependencies. Applications that need the framework mapper policy without configuration loading can use `com.networknt.jackson.JacksonDefaults`.

- `mapper()` returns the shared immutable JSON mapper.
- `builder()` creates an independent JSON builder.
- `configure(factoryBuilder)` applies Jackson 2 stream defaults and measured limits to a JSON, YAML or XML factory.
- `configure(mapperBuilder)` applies the measured common mapper policy. Apply application-specific overrides afterwards.

Format-specific schemas and read/write features remain the format builder's responsibility. For example:

```java
var factory = JacksonDefaults.configure(YAMLFactory.builder()).build();
var mapper = JacksonDefaults.configure(YAMLMapper.builder(factory)).build();
```

The existing `com.networknt.config.Jackson2Defaults` API remains a delegating compatibility facade, including its original return types. The lightweight module independently tests the frozen feature profile, stream limits and private-builder isolation. Config tests verify facade identity and the existing Config snapshots. Cross-format parity is also checked by the SOAP plugin and generator consumers.

Source-build consumers may set `JACKSON3_REUSE_CORE_QUALIFICATION=1` with `JACKSON3_CORE_QUALIFICATION_REPORT` to require the latest successful full-core workflow for the exact clean checkout SHA. Missing, pending or failed qualification blocks installation. The core workflow leaves this option unset and always runs its full tests. Consumers still test their own sources and all other prerequisites.
