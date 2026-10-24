package com.networknt.config.yml;

import com.networknt.config.Config;
import com.networknt.decrypt.Decryptor;
import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class YamlEngineTest {
    @Test
    void sharedReaderHelpersKeepConcurrentDocumentsIsolated() throws Exception {
        var reader = YamlUtils.newLoad();
        var tasks = new ArrayList<Callable<Map<String, Object>>>();
        for (int index = 0; index < 80; index++) {
            int id = index;
            tasks.add(() -> {
                String yaml = "id: " + id + "\nnested: [yes, true]\n";
                return id % 2 == 0 ? YamlUtils.loadMap(reader, yaml)
                        : YamlUtils.loadMap(reader, new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
            });
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            var results = executor.invokeAll(tasks);
            for (int index = 0; index < results.size(); index++) {
                assertEquals(Map.of("id", index, "nested", List.of("yes", true)), results.get(index).get());
            }
        }
    }

    @Test
    void publicReaderUsesYaml12CoreScalars() {
        var yaml = Config.getNoneDecryptedInstance().getYaml();
        Map<?, ?> values;
        synchronized (yaml) {
            values = (Map<?, ?>) yaml.loadFromString("""
                    enabled: TRUE
                    disabled: False
                    absent: ~
                    decimal: 012
                    octal: 0o12
                    fraction: 1.25
                    legacy: [yes, no, on, off, 2026-10-24, 'true']
                    """);
        }
        assertEquals(true, values.get("enabled"));
        assertEquals(false, values.get("disabled"));
        assertNull(values.get("absent"));
        assertEquals(12, values.get("decimal"));
        assertEquals(10, values.get("octal"));
        assertEquals(1.25, values.get("fraction"));
        assertEquals(List.of("yes", "no", "on", "off", "2026-10-24", "true"), values.get("legacy"));
    }

    @Test
    void anchorsAndMergeKeysRetainExplicitOverrides() {
        Map<?, ?> values = (Map<?, ?>) YamlUtils.newLoad().loadFromString("""
                defaults: &defaults
                  enabled: true
                  timeout: 10
                service:
                  <<: *defaults
                  timeout: 20
                """);
        assertEquals(Map.of("enabled", true, "timeout", 20), values.get("service"));
    }

    @Test
    void decryptsNestedPlainScalarsButLeavesQuotedAndPlainReadersUnchanged() {
        String source = """
                password: CRYPT:example
                nested: [CRYPT:child]
                mapping: {secret: 'CRYPT:quoted'}
                """;
        var yaml = YamlUtils.newLoad(FixtureDecryptor.class.getName());
        Map<?, ?> decrypted = (Map<?, ?>) yaml.loadFromInputStream(
                new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        assertEquals("decoded-example", decrypted.get("password"));
        assertEquals(List.of("decoded-child"), decrypted.get("nested"));
        assertEquals(Map.of("secret", "CRYPT:quoted"), decrypted.get("mapping"));
        Map<?, ?> plain = (Map<?, ?>) YamlUtils.newLoad().loadFromString(source);
        assertEquals("CRYPT:example", plain.get("password"));
        assertEquals(List.of("CRYPT:child"), plain.get("nested"));
    }

    @Test
    void rejectsArbitraryJavaTags() {
        for (var yaml : List.of(YamlUtils.newLoad(), YamlUtils.newLoad(FixtureDecryptor.class.getName()))) {
            assertThrows(YamlEngineException.class, () -> yaml.loadFromString("!!java.net.URL 'https://example.com'"));
        }
    }

    @Test
    void duplicateKeysKeepLastValueWithStrictOptIn() {
        String previous = System.getProperty(YamlUtils.ALLOW_DUPLICATE_KEYS);
        try {
            System.clearProperty(YamlUtils.ALLOW_DUPLICATE_KEYS);
            for (var yaml : List.of(YamlUtils.newLoad(), YamlUtils.newLoad(FixtureDecryptor.class.getName()))) {
                assertEquals(Map.of("key", "two"), yaml.loadFromString("key: one\nkey: two\n"));
            }
            System.setProperty(YamlUtils.ALLOW_DUPLICATE_KEYS, "false");
            for (var yaml : List.of(YamlUtils.newLoad(), YamlUtils.newLoad(FixtureDecryptor.class.getName()))) {
                assertThrows(YamlEngineException.class, () -> yaml.loadFromString("key: one\nkey: two\n"));
            }
        } finally {
            if (previous == null) System.clearProperty(YamlUtils.ALLOW_DUPLICATE_KEYS);
            else System.setProperty(YamlUtils.ALLOW_DUPLICATE_KEYS, previous);
        }
    }

    @Test
    void numericYaml12ChangesAreExplicit() {
        Map<?, ?> values = (Map<?, ?>) YamlUtils.newLoad().loadFromString("""
                octalBefore: 0755
                octalNow: 0o755
                binaryBefore: 0b11
                separatorBefore: 1_000
                sexagesimalBefore: 1:30
                sexagesimalFloatBefore: 1:30.5
                floatSeparatorBefore: 1_0.5
                exponentNow: 1e3
                hexadecimal: 0xFF
                """);
        assertEquals(755, values.get("octalBefore"));
        assertEquals(493, values.get("octalNow"));
        assertEquals("0b11", values.get("binaryBefore"));
        assertEquals("1_000", values.get("separatorBefore"));
        assertEquals("1:30", values.get("sexagesimalBefore"));
        assertEquals("1:30.5", values.get("sexagesimalFloatBefore"));
        assertEquals("1_0.5", values.get("floatSeparatorBefore"));
        assertEquals(1000.0, values.get("exponentNow"));
        assertEquals(255, values.get("hexadecimal"));
    }

    @Test
    void retainsCollectionAliasLimit() {
        String source = "anchor: &anchor [one]\nreferences: [" + "*anchor,".repeat(51) + "]\n";
        var reader = YamlUtils.newLoad();
        assertThrows(YamlEngineException.class, () -> reader.loadFromString(source));
    }

    @Test
    void blockWriterRoundTripsStringTypesAndEncryptedValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("strings", List.of("true", "null", "012", "", "yes", "on", "${values.key:default}"));
        values.put("unicode", "configuration café");
        values.put("multiline", "first\nsecond\n");
        values.put("secret", "CRYPT:example");
        values.put("enabled", true);
        String output = YamlUtils.newDump().dumpToString(values);
        assertEquals(values, YamlUtils.newLoad().loadFromString(output));
        Map<?, ?> decrypted = (Map<?, ?>) YamlUtils.newLoad(FixtureDecryptor.class.getName()).loadFromString(output);
        assertEquals("decoded-example", decrypted.get("secret"));
        assertEquals(values.get("strings"), decrypted.get("strings"));
    }

    public static final class FixtureDecryptor implements Decryptor {
        @Override
        public String decrypt(String input) {
            return "decoded-" + input.substring("CRYPT:".length());
        }
    }
}
