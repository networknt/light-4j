package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.info.ServerInfoConfig;
import com.networknt.info.ServerInfoUtil;
import com.networknt.proxy.mras.MrasConfig;
import com.networknt.proxy.salesforce.SalesforceConfig;
import com.networknt.server.ModuleRegistry;
import com.networknt.server.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CredentialRegistryMaskingTest {
    private boolean originalMasking;

    @BeforeEach
    void enableMasking() {
        originalMasking = ServerConfig.getInstance().isMaskConfigProperties();
        ServerConfig.getInstance().setMaskConfigProperties(true);
    }

    @AfterEach
    void restoreConfig() {
        Config.getInstance().setClassLoader(Config.class.getClassLoader());
        Config.getNoneDecryptedInstance().setClassLoader(Config.class.getClassLoader());
        ServerConfig.getInstance().setMaskConfigProperties(originalMasking);
        MrasConfig.load();
        SalesforceConfig.load();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void masksMrasCredentialsWithoutChangingRuntimeValues(boolean stringSections) {
        Map<String, Object> basic = Map.of("username", "basic-user", "password", "basic-secret");
        Map<String, Object> token = Map.of("username", "token-user", "password", "token-secret");
        Map<String, Object> microsoft = Map.of("clientId", "client-id", "clientSecret", "microsoft-secret");
        installConfig("mras", Map.of("enabled", true,
                "keyStorePass", "keystore-secret", "keyPass", "key-secret", "trustStorePass", "truststore-secret",
                "basicAuth", section(basic, stringSections), "accessToken", section(token, stringSections),
                "microsoft", section(microsoft, stringSections)));

        MrasConfig runtime = MrasConfig.load();
        Map<String, Object> registered = registered("mras", MrasConfig.class);
        assertEquals("*", registered.get("keyStorePass"));
        assertEquals("*", registered.get("keyPass"));
        assertEquals("*", registered.get("trustStorePass"));
        assertSection(registered.get("basicAuth"), "password", "username", "basic-user", stringSections);
        assertSection(registered.get("accessToken"), "password", "username", "token-user", stringSections);
        assertSection(registered.get("microsoft"), "clientSecret", "clientId", "client-id", stringSections);
        assertEquals("keystore-secret", runtime.getKeyStorePass());
        assertEquals("key-secret", runtime.getKeyPass());
        assertEquals("truststore-secret", runtime.getMappedConfig().get("trustStorePass"));
        assertEquals("basic-secret", runtime.getBasicAuth().get("password"));
        assertEquals("token-secret", runtime.getAccessToken().get("password"));
        assertEquals("microsoft-secret", runtime.getMicrosoft().get("clientSecret"));
        assertNoSecretsInPublishedConfig("mras", registered, "keystore-secret", "key-secret", "truststore-secret",
                "basic-secret", "token-secret", "microsoft-secret");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void masksSalesforceCredentialsWithoutChangingRuntimeValues(boolean stringSections) {
        List<Map<String, Object>> auths = List.of(Map.of("pathPrefix", "/salesforce", "grantType", "password",
                "password", "password-secret", "clientSecret", "client-secret", "certPassword", "nested-cert-secret"));
        installConfig("salesforce", Map.of("enabled", true, "certPassword", "certificate-secret",
                "pathPrefixAuths", section(auths, stringSections)));

        SalesforceConfig runtime = SalesforceConfig.load();
        Map<String, Object> registered = registered("salesforce", SalesforceConfig.class);
        assertEquals("*", registered.get("certPassword"));
        if (stringSections) {
            assertEquals("*", registered.get("pathPrefixAuths"));
        } else {
            Map<?, ?> auth = (Map<?, ?>) ((List<?>) registered.get("pathPrefixAuths")).get(0);
            assertEquals("/salesforce", auth.get("pathPrefix"));
            assertEquals("*", auth.get("password"));
            assertEquals("*", auth.get("clientSecret"));
            assertEquals("*", auth.get("certPassword"));
        }
        assertEquals("certificate-secret", runtime.getCertPassword());
        assertEquals("password-secret", runtime.getPathPrefixAuths().get(0).getPassword());
        assertEquals("client-secret", runtime.getPathPrefixAuths().get(0).getClientSecret());
        assertNoSecretsInPublishedConfig("salesforce", registered, "certificate-secret", "password-secret",
                "client-secret", "nested-cert-secret");
    }

    private static Object section(Object value, boolean stringSection) {
        return stringSection ? JsonMapper.toJson(value) : value;
    }

    private static void assertSection(Object section, String secretKey, String publicKey, String publicValue, boolean stringSection) {
        if (stringSection) {
            assertEquals("*", section);
        } else {
            assertEquals("*", ((Map<?, ?>) section).get(secretKey));
            assertEquals(publicValue, ((Map<?, ?>) section).get(publicKey));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> registered(String name, Class<?> configClass) {
        return (Map<String, Object>) ModuleRegistry.getModuleRegistry().get(name + ":" + configClass.getName());
    }

    private static void assertNoSecretsInPublishedConfig(String name, Map<String, Object> registered, String... secrets) {
        assertEquals(Boolean.TRUE, registered.get("enabled"));
        Map<?, ?> components = (Map<?, ?>) ServerInfoUtil.getServerInfo(ServerInfoConfig.load()).get("component");
        assertNotNull(components.get(name));
        String registryJson = JsonMapper.toJson(registered);
        String infoJson = JsonMapper.toJson(components.get(name));
        for (String secret : secrets) {
            assertFalse(registryJson.contains(secret));
            assertFalse(infoJson.contains(secret));
        }
    }

    private static void installConfig(String name, Map<String, Object> values) {
        byte[] content = JsonMapper.toJson(values).getBytes(StandardCharsets.UTF_8);
        ClassLoader loader = new ClassLoader(Config.class.getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String resource) {
                if (resource.equals(name + ".yml") || resource.equals("config/" + name + ".yml")) {
                    return new ByteArrayInputStream(content);
                }
                return super.getResourceAsStream(resource);
            }
        };
        Config.getInstance().setClassLoader(loader);
        Config.getNoneDecryptedInstance().setClassLoader(loader);
    }
}
