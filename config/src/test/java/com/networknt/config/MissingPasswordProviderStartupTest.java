package com.networknt.config;

import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.MissingMasterPasswordProviderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Surefire runs each startup test class in its own JVM (reuseForks=false). */
class MissingPasswordProviderStartupTest {
    @TempDir Path directory;

    @Test
    void configYmlRejectsBareBaseBeforeReadingAnyCryptValue() throws Exception {
        assertTrue(System.getenv("light_4j_config_password") == null, "Runtime password environment must be excluded");
        assertTrue(System.getenv("LIGHT_4J_CONFIG_PASSWORD") == null, "Runtime password environment must be excluded");
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + AESSaltDecryptor.class.getName());
        String previous = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
        try {
            System.setProperty(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
            var failure = assertThrows(ExceptionInInitializerError.class, Config::getInstance);
            assertInstanceOf(MissingMasterPasswordProviderException.class, failure.getCause());
            assertThrows(NoClassDefFoundError.class, Config::getInstance);
        } finally {
            if (previous == null) System.clearProperty(Config.LIGHT_4J_CONFIG_DIR);
            else System.setProperty(Config.LIGHT_4J_CONFIG_DIR, previous);
        }
    }
}
