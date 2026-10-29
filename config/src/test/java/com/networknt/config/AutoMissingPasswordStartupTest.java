package com.networknt.config;

import com.networknt.decrypt.AutoAESSaltDecryptor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** A dedicated Surefire fork has no runtime password and no module-wide fixture property. */
class AutoMissingPasswordStartupTest {
    @Test
    void autoRejectsMissingPasswordOnAThreadWithoutJUnitFrames() throws Exception {
        assertTrue(System.getProperty("light_4j_config_password") == null, "No fixture password may be injected");
        assertTrue(System.getProperty("LIGHT_4J_CONFIG_PASSWORD") == null, "No fixture password may be injected");
        assertTrue(System.getenv("light_4j_config_password") == null, "Runtime password environment must be excluded");
        assertTrue(System.getenv("LIGHT_4J_CONFIG_PASSWORD") == null, "Runtime password environment must be excluded");
        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> new AutoAESSaltDecryptor().decrypt(
                    "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5"));
            var failure = assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));
            assertInstanceOf(RuntimeException.class, failure.getCause());
            assertTrue(failure.getCause().getMessage().contains("Unable to retrieve decrypted password"));
        }
    }
}
