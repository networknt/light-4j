package com.networknt.config;

import com.networknt.decrypt.AutoAESSaltDecryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Surefire supplies the explicit fixture password and a fresh JVM for this class. */
class ExplicitPasswordProviderStartupTest {
    @TempDir Path directory;

    @Test
    void explicitProviderLoadsCryptThroughPublicConfig() throws Exception {
        assertTrue("light".equals(System.getProperty("light_4j_config_password")), "Expected explicit synthetic fixture password");
        assertTrue(System.getenv("light_4j_config_password") == null, "Runtime password environment must be excluded");
        assertTrue(System.getenv("LIGHT_4J_CONFIG_PASSWORD") == null, "Runtime password environment must be excluded");
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + AutoAESSaltDecryptor.class.getName());
        Files.writeString(directory.resolve("provider-secret.yml"),
                "password: CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5");
        String previous = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
        try {
            System.setProperty(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
            assertEquals("password", Config.getInstance().getJsonMapConfig("provider-secret").get("password"));
        } finally {
            if (previous == null) System.clearProperty(Config.LIGHT_4J_CONFIG_DIR);
            else System.setProperty(Config.LIGHT_4J_CONFIG_DIR, previous);
        }
    }
}
