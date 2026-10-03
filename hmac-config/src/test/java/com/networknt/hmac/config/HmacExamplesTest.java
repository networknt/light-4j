/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.hmac.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.generator.YamlGenerator;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HmacExamplesTest {
    @Test
    void examplesAndFieldHelpMatchGeneratedMetadata() throws Exception {
        var annotation = HmacConfig.class.getDeclaredField("profiles").getAnnotation(MapField.class);
        var mapper = Config.getInstance().getMapper();
        try (var json = getClass().getResourceAsStream("/config/examples/hmac-profiles.json");
             var input = getClass().getResourceAsStream("/config/hmac-schema.json")) {
            assertNotNull(json);
            assertNotNull(input);
            var expected = mapper.readTree(json);
            var schema = mapper.readTree(input);
            var profiles = schema.path("properties").path("profiles");
            assertEquals(annotation.description(), profiles.path("description").asText());
            assertEquals(1, annotation.examples().length);
            assertEquals(expected, mapper.readTree(annotation.examples()[0]));
            assertEquals(expected, profiles.path("examples").get(0));
            for (String extension : new String[]{"yml", "yaml"}) {
                try (var yaml = getClass().getResourceAsStream("/config/hmac." + extension)) {
                    assertNotNull(yaml);
                    String content = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);
                    assertTrue(content.contains("# " + YamlGenerator.FIELD_REFERENCE_HEADER));
                    assertEquals("Value fields (required/optional, defaults and constraints):",
                            YamlGenerator.FIELD_REFERENCE_HEADER);
                    assertTrue(content.contains("#     defaultEnvNames: []"));
                    assertTrue(content.contains("#     bySelector: {}"));
                    var profileFields = profiles.path("additionalProperties").path("properties");
                    profileFields.fields().forEachRemaining(entry -> {
                        if (entry.getKey().equals("secrets") || entry.getKey().equals("replay")) {
                            entry.getValue().path("properties").fields().forEachRemaining(child ->
                                    assertTrue(content.contains("# " + entry.getKey() + "." + child.getKey()
                                            + ": " + child.getValue().path("description").asText())));
                        } else assertTrue(content.contains("# " + entry.getKey() + ": "
                                + entry.getValue().path("description").asText()));
                    });
                    // The generator's stable example marker separates help from data.
                    String marker = "# " + YamlGenerator.EXAMPLE_HEADER + "\n";
                    assertTrue(content.contains(marker), "Missing generated example marker");
                    String[] sections = content.split(java.util.regex.Pattern.quote(marker), 2);
                    StringBuilder example = new StringBuilder();
                    for (String line : sections[1].split("\n")) {
                        if (!line.startsWith("#") || line.equals("# " + YamlGenerator.FIELD_REFERENCE_HEADER)) break;
                        example.append(line.replaceFirst("^# ?", "")).append('\n');
                    }
                    assertEquals(expected, mapper.valueToTree(new Yaml().load(example.toString())));
                }
            }
            StringBuilder table = new StringBuilder("<!-- profile-fields:start -->\n")
                    .append("| Profile field | Requirement, default and behavior |\n")
                    .append("| --- | --- |\n");
            profiles.path("additionalProperties").path("properties").fields().forEachRemaining(entry -> {
                if (entry.getKey().equals("secrets") || entry.getKey().equals("replay")) {
                    entry.getValue().path("properties").fields().forEachRemaining(child ->
                            appendField(table, entry.getKey() + "." + child.getKey(), child.getValue()));
                } else appendField(table, entry.getKey(), entry.getValue());
            });
            table.append("<!-- profile-fields:end -->");
            assertTrue(Files.readString(readmePath(System.getProperty("basedir")))
                    .contains(table.toString()), "README field reference must match generated descriptions");
        }
    }

    @Test
    void locatesReadmeWithoutMavenBasedirForIdeRuns() throws Exception {
        assertTrue(Files.isRegularFile(readmePath(null)));
    }

    private static Path readmePath(String basedir) throws Exception {
        var module = basedir == null
                ? Path.of(HmacExamplesTest.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).getParent().getParent()
                : Path.of(basedir);
        return module.resolve("README.md");
    }

    private static void appendField(StringBuilder table, String name, JsonNode field) {
        table.append("| `").append(name).append("` | ")
                .append(field.path("description").asText().replace("|", "\\|"))
                .append(" |\n");
    }

    @Test
    void loadsDocumentedProfilesAsAMap() throws Exception {
        verifyExample(false);
    }

    @Test
    void loadsDocumentedProfilesAsAJsonString() throws Exception {
        verifyExample(true);
    }

    private void verifyExample(boolean jsonString) throws Exception {
        String name = "hmac-documentation-" + jsonString;
        try (var input = getClass().getResourceAsStream("/config/examples/hmac-profiles.json")) {
            assertNotNull(input);
            var mapper = Config.getInstance().getMapper();
            var profiles = mapper.readValue(input, Map.class);
            Config.getInstance().putInConfigCache(name, Map.of(
                    "enabled", true,
                    "profiles", jsonString ? mapper.writeValueAsString(profiles) : profiles));
            HmacConfig config = HmacConfig.load(name);
            assertEquals(2, config.getProfiles().size());
            var github = config.getProfiles().get("github");
            assertEquals("X-Hub-Signature-256", github.getSignatureHeader());
            assertEquals(2, github.getSecrets().getBySelector().get("12345678").size());
            assertTrue(github.getSecrets().getDefaultEnvNames().isEmpty());
            assertTrue(github.getReplay().isEnabled());
            var shared = config.getProfiles().get("shared-build");
            assertEquals("base64", shared.getSignatureEncoding());
            assertTrue(shared.getSecrets().getBySelector().isEmpty());
            assertEquals(1, shared.getSecrets().getDefaultEnvNames().size());
            assertFalse(shared.getReplay().isEnabled());
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }
}
