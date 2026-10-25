# Shared Jackson compatibility policy

`com.networknt:jackson-defaults` depends only on Jackson databind and its core/annotation dependencies. Applications that need the framework mapper policy without configuration loading can use `com.networknt.jackson.Jackson2Defaults`.

- `mapper()` returns the shared immutable JSON mapper.
- `builder()` creates an independent JSON builder.
- `configure(factoryBuilder)` applies Jackson 2 stream defaults and measured limits to a JSON, YAML or XML factory.
- `configure(mapperBuilder)` applies the measured common mapper policy. Apply application-specific overrides afterwards.

Format-specific schemas and read/write features remain the format builder's responsibility. For example:

```java
var factory = Jackson2Defaults.configure(YAMLFactory.builder()).build();
var mapper = Jackson2Defaults.configure(YAMLMapper.builder(factory)).build();
```

The existing `com.networknt.config.Jackson2Defaults` API remains a delegating compatibility facade, including its original return types. Config's feature snapshots and stream-limit tests exercise the facade and shared implementation. Cross-format parity is also checked by the SOAP plugin and generator consumers.
