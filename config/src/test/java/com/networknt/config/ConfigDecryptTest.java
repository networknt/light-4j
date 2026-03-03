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

package com.networknt.config;

import com.networknt.config.yml.DecryptConstructor;
import com.networknt.config.yml.YmlConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class ConfigDecryptTest {
	private static final String SECRET="password";
	private static final String TEST_MAP="testMap";
	private static final String TEST_ARRAY="testArray";

    @SuppressWarnings("unchecked")
	@Test
    @Disabled
    public void testDecryptMap() {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfigNoCache("secret-map-test");
        Assertions.assertEquals(SECRET, secretMap.get("serverKeystorePass"));

        secretMap = Config.getInstance().getJsonMapConfig("secret-map-test");
        Assertions.assertEquals(SECRET, secretMap.get("serverKeystorePass"));

        List<String> testArray = (List<String>) secretMap.get(TEST_ARRAY);

        Assertions.assertTrue(testArray.size()>0);

        for (String s: testArray) {
        	Assertions.assertEquals(SECRET, s);
        }

        Map<String, String> testMap = (Map<String, String>) secretMap.get(TEST_MAP);

        Assertions.assertTrue(testMap.size()>0);

        for (String s: testMap.values()) {
        	Assertions.assertEquals(SECRET, s);
        }
    }

    @Test
    @Disabled
    public void testDecryptObject() {
    	SecretConfig secretConfig = (SecretConfig) Config.getInstance().getJsonObjectConfig("secret-object-test", SecretConfig.class);

    	Assertions.assertEquals(SECRET, secretConfig.getServerKeystorePass());
    	Assertions.assertEquals(SECRET, secretConfig.getServerKeyPass());
    	Assertions.assertEquals(SECRET, secretConfig.getServerTruststorePass());
    	Assertions.assertEquals(SECRET, secretConfig.getClientKeystorePass());
    	Assertions.assertEquals(SECRET, secretConfig.getClientKeyPass());
    	Assertions.assertEquals(SECRET, secretConfig.getClientTruststorePass());

        Assertions.assertTrue(secretConfig.getTestArray().size()>0);

        for (String s: secretConfig.getTestArray()) {
        	Assertions.assertEquals(SECRET, s);
        }

        Assertions.assertTrue(secretConfig.getTestMap().size()>0);

        for (String s: secretConfig.getTestMap().values()) {
        	Assertions.assertEquals(SECRET, s);
        }
    }

    @Test
    public void testDecryptorClass() {
        final Resolver resolver = new Resolver();
        resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
        Yaml yaml = new Yaml(DecryptConstructor.getInstance("com.networknt.config.TestDecryptor"), new Representer(new DumperOptions()), new DumperOptions(), resolver);

        Map<String, Object> secret=yaml.load(Config.getInstance().getInputStreamFromFile("secret-map-test2.yml"));

        Assertions.assertEquals(SECRET+"-test", secret.get("serverKeystorePass"));
    }

    @Test
    public void testAutoDecryptorClass() throws IOException {
        if (System.getenv("config_password") == null || !System.getenv("config_password").equals("light")) return;
        final Resolver resolver = new Resolver();
        resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
        Yaml yaml = new Yaml(DecryptConstructor.getInstance("com.networknt.config.TestAutoDecryptor"), new Representer(new DumperOptions()), new DumperOptions(), resolver);

        Map<String, Object> secret=yaml.load(Config.getInstance().getInputStreamFromFile("secret-map-test2.yml"));

        Assertions.assertEquals(SECRET+"-test", secret.get("serverKeystorePass"));
    }
}
