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
Object parsed;
synchronized (reader) {
    parsed = reader.loadFromString(yamlText); // or loadFromInputStream(stream)
}
MyConfig bean = config.getMapper().convertValue(parsed, MyConfig.class);
String text = YamlUtils.newDump().dumpToString(configMap);
```

Import `com.networknt.config.yml.YamlUtils` for fresh readers and block-style
writers. Replace `Yaml.load`, `loadAs`, and `dump` with the corresponding Engine
operations above. Custom `Config` providers must return an Engine `Load` from
`getYaml()`. `DecryptConstructor` now extends Engine's `StandardConstructor`;
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
access and centralize the mapping cast. Raw Engine `Load` instances still
require synchronization when used directly. Writers support `Date`, `Calendar`
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
