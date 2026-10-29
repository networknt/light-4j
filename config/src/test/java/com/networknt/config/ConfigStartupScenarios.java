package com.networknt.config;

import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.AutoAESSaltDecryptor;
import com.networknt.decrypt.MissingMasterPasswordProviderException;
import com.networknt.test.SystemPropertyScope;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Public scenario entry point, loaded with fresh application static state by each test. */
public final class ConfigStartupScenarios {
    public static void run(String mode, Path directory) throws Exception {
        String provider = mode.equals("valid") ? AutoAESSaltDecryptor.class.getName() : AESSaltDecryptor.class.getName();
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + provider);
        Files.writeString(directory.resolve("provider-secret.yml"),
                "password: CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5");
        try (var configDirectory = new SystemPropertyScope(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
             var fixturePassword = new SystemPropertyScope("light_4j_config_password", "light")) {
            if (mode.equals("valid")) {
                assertEquals("password", Config.getInstance().getJsonMapConfig("provider-secret").get("password"));
            } else {
                var failure = assertThrows(ExceptionInInitializerError.class, Config::getInstance);
                assertInstanceOf(MissingMasterPasswordProviderException.class, failure.getCause());
                assertNull(failure.getCause().getCause(), "Provider failures must not carry a synthetic marker cause");
                assertThrows(NoClassDefFoundError.class, Config::getInstance);
            }
        }
    }
}
