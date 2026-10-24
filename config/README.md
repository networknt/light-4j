# YAML configuration migration

The config module uses `org.snakeyaml:snakeyaml-engine:3.0.1` for loading and
writing. It also retains its compile-scope export of classic SnakeYAML 2.7 for
downstream applications that still import that API without declaring it.

## Release gate

This migration requires a coordinated **major release**. The public API changes
below are intentionally source and binary incompatible; recompile applications
and custom `Config` service providers. Do not publish these changes as a 2.x
patch release. The repository's `2.4.2-SNAPSHOT` versions are deliberately left
unchanged pending the owner's coordinated version update across repositories.

`Config.getYaml()` now returns `org.snakeyaml.engine.v2.api.Load`. Engine
separates parsing from writing and does not construct Java beans from YAML tags:

```java
var config = Config.getInstance();
var reader = config.getYaml();
Object parsed = YamlUtils.load(reader, yamlText); // or YamlUtils.load(reader, stream)
MyConfig bean = config.getMapper().convertValue(parsed, MyConfig.class);
String text = YamlUtils.newDump().dumpToString(configMap);
```

Import `com.networknt.config.yml.YamlUtils` for fresh readers and block-style
writers. Replace `Yaml.load`, `loadAs`, and `dump` with the corresponding Engine
operations above. Custom `Config` providers must return an Engine `Load` from
`getYaml()`. `DecryptConstructor` now extends Engine's `StandardConstructor`;
its `create()`/`create(String)` factories replace `getInstance` and return fresh
constructors, which must not be shared between readers.
`ConfigLoaderConstructor` is a plain factory/holder with no YAML superclass.
`YmlConstants.CRYPT_TAG` is an Engine tag.
`YamlGenerator.YamlCommentRepresenter` accepts Engine `DumpSettings`.

Readers and writers explicitly use YAML 1.2 Core scalar rules:

- `true`/`false` (including case variants) are booleans. `yes`, `no`, `on`, and
  `off` are strings; update configurations that used these as booleans.
- Leading-zero integers such as `012` are decimal. Use `0o12` for octal.
- Dates and timestamps remain strings. Quote numeric-looking values when a
  string is required.
- Duplicate mapping keys retain the classic last-value-wins policy by default.
  Set `-Dlight-4j-yaml-allow-duplicate-keys=false` before creating config readers
  to opt into strict rejection. Collection-valued keys and arbitrary Java
  object tags are rejected. Anchor
  aliases and merge keys remain supported, with a limit of 50 collection aliases.
- Unquoted `CRYPT:` values use the configured decryptor, including in nested
  maps and lists. Quoted values and non-decrypted readers retain the original
  text. Both injection exclusions and normal object loading bind parsed data
  through the configured Jackson mapper.

The numeric grammar changes include the following (quoted forms always remain
strings):

| YAML scalar | Classic YAML 1.1 | Engine YAML 1.2 Core |
| --- | --- | --- |
| `0755` | integer 493 | integer 755 |
| `0o755` | string | integer 493 |
| `0b11` | integer 3 | string |
| `1_000` | integer 1000 | string |
| `1:30` | integer 90 | string |
| `1:30.5` | number 90.5 | string |
| `1_0.5` | number 10.5 | string |
| `1e3` | number 1000.0 | number 1000.0 |
| `0xFF` | integer 255 | integer 255 |

Audit binary, underscore-separated and sexagesimal numbers before upgrading;
rewrite them as decimal numbers when numeric values are intended. Exponent
notation also follows Core rules. The normative scalar grammar is the
[YAML 1.2 Core schema](https://yaml.org/spec/1.2.2/#103-core-schema).
Engine treats `%YAML 1.1` directives using its YAML 1.2 schema too; a directive
does not restore classic resolution.

Use `YamlUtils.load` and `loadMap` to share readers safely; they serialize
access with privately owned per-reader monitors and centralize the mapping cast.
Use these helpers consistently for shared readers; do not mix direct Engine
calls with helper calls. Raw Engine `Load` instances are not thread safe.
Writers support `Date`, `Calendar`
and Java temporal values as ISO strings. Unsupported object types fail the
fallback-file write without invalidating the active config cache.

Generated configuration files retain comments, examples, placeholders, and
block formatting. Engine writers quote strings that could otherwise be read as
Core scalars.

The parent also manages classic SnakeYAML 2.7 for the external `yaml-rule`
dependency, whose Jackson YAML implementation requires it. This migration does
not replace that external parser. Rule bodies are bound directly through its
configured mapper, without a JSON/YAML text round trip. Strings, Unicode line
separators, and long map keys therefore retain their original data.


## Jackson 3 migration

The development line uses `tools.jackson.core:jackson-databind:3.2.3` under the
`tools.jackson:jackson-bom`. Jackson annotations retain their
`com.fasterxml.jackson.annotation` package. Jackson 3 requires Java 17 or later;
this repository retains its existing Java 25 build target.

`Config.getMapper()` and `JsonMapper.objectMapper` now expose Jackson 3 types.
This is a source and binary incompatible API change. Coordinate downstream
migration and a next-major release before publishing; project versions are not
changed by this implementation. The pre-migration `2.x` maintenance branch
preserves the old API.

Mappers are immutable. Configure a builder before building a private mapper:

```java
// Before (Jackson 2)
ObjectMapper mapper = new ObjectMapper();
mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

// After (Jackson 3, tools.jackson.databind imports)
ObjectMapper mapper = Jackson2Defaults.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
```

The framework mappers supplement Jackson's documented partial compatibility
profile with measured shared-feature settings. `Config.getMapper()` continues
to reject unknown properties and empty beans and include null properties.
`JsonMapper.objectMapper` continues to ignore unknown properties, permit empty
beans, omit null properties and null map entries, and write plain BigDecimals. Both retain legacy
property ordering, primitive-null coercion, trailing-token handling,
getter-backed collection binding, ISO date/time output and numeric durations.
Java time and JDK8 support are built into Jackson 3; separate modules are removed.
The frozen feature tests cover common named flags, not removed/renamed settings
or all possible application types.

Jackson failures are now unchecked `JacksonException` instances. JSON helper
methods retain RuntimeException wrapping, and config binding retains the
configuration-name error context and original cause. Stream I/O catches remain.
Malformed JSON now reports StreamReadException instead of JsonParseException.

Config YAML continues through the existing SnakeYAML Engine loader; this phase
does not change that loader's scalar policy. The Jackson YAML resolver used by
json-overlay and yaml-rule has its own documented scalar changes. Classic
SnakeYAML pins remain until the plan's final dependency cleanup phase.

### Accepted ClientConfig binding difference

Jackson 2.22.3 rejects direct typed ClientConfig binding because `getOAuth()`
and `getOauthConfig()` conflict on the `oauth` property. Jackson 3 resolves that
getter conflict. Four measured client.yml fixtures now bind successfully; a
fifth reaches an unknown-property rejection instead of failing introspection.
This difference was explicitly accepted during the migration. The raw config
map output is unchanged. The fixtures are in light-4j modules `client`,
`deref-token`, `portal-registry`, `security` (successful bindings) and `rule-loader`
(unknown-property rejection).


Direct object binding of `JwtConfig` and `SanitizerConfig` remains unsupported
for the measured object fixtures: neither class exposes an object creator for
Jackson. Jackson 3 reports `InvalidDefinitionException` where Jackson 2 reported
`MismatchedInputException`. Both remain binding failures; the config map and
supported manual loading paths are separate from this direct-binding probe.


The public `Jackson2Defaults.builder()` supplies the common measured policy;
`Jackson2Defaults.mapper()` reuses a shared immutable mapper when no overrides
are needed. The config, HTTP, JSONPath and other framework consumers use this
profile rather than duplicating Jackson's partial compatibility builder.

Stream constraints are explicitly retained from Jackson 2.22.3: read and write
nesting depth 1,000, string length 20,000,000, field-name length 50,000 and number
length 1,000. Document length and token count remain unlimited. These constraints
are tested separately from the 84 common feature flags, including actual nested
JSON and oversized-string reads. JSONPath uses the same profile and retains
permissive trailing-token handling. MCP configuration binding failures continue
to return JSON-RPC invalid params (-32602).
