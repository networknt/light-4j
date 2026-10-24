# YAML configuration migration

The config module uses `org.snakeyaml:snakeyaml-engine:3.0.1`. It no longer
exports classic SnakeYAML. Recompile applications and custom `Config` service
providers against this version.

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
`getYaml()`. `DecryptConstructor` and `ConfigLoaderConstructor` now extend
Engine's `StandardConstructor`; `YmlConstants.CRYPT_TAG` is an Engine tag.
`YamlGenerator.YamlCommentRepresenter` accepts Engine `DumpSettings`.

Readers and writers explicitly use YAML 1.2 Core scalar rules:

- `true`/`false` (including case variants) are booleans. `yes`, `no`, `on`, and
  `off` are strings; update configurations that used these as booleans.
- Leading-zero integers such as `012` are decimal. Use `0o12` for octal.
- Dates and timestamps remain strings. Quote numeric-looking values when a
  string is required.
- Duplicate mapping keys, collection-valued keys, and arbitrary Java object tags
  are rejected. Anchor
  aliases and merge keys remain supported, with a limit of 50 collection aliases.
- Unquoted `CRYPT:` values use the configured decryptor, including in nested
  maps and lists. Quoted values and non-decrypted readers retain the original
  text. Both injection exclusions and normal object loading bind parsed data
  through the configured Jackson mapper.

Generated configuration files retain comments, examples, placeholders, and
block formatting. Engine writers quote strings that could otherwise be read as
Core scalars.

The parent still manages classic SnakeYAML 2.7 for the external `yaml-rule`
dependency, whose Jackson YAML implementation requires it. This migration does
not replace that external parser or remove classic SnakeYAML from applications
that use `yaml-rule`. Rule bodies are transferred to its parser as JSON (a YAML
subset) so strings such as `yes` and `on` retain their types at that boundary.
