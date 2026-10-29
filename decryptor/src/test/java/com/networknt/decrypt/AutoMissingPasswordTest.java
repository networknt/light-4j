package com.networknt.decrypt;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AutoMissingPasswordTest {
    @Test
    void autoRejectsAbsentRuntimePasswordOnAThreadWithoutJUnitFrames() throws Exception {
        AutoAESSaltDecryptor decryptor = new AutoAESSaltDecryptor() {
            @Override String getRuntimePassword() { return null; }
        };
        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> decryptor.decrypt(
                    "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5"));
            var failure = assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));
            assertInstanceOf(RuntimeException.class, failure.getCause());
            assertTrue(failure.getCause().getMessage().contains("Unable to retrieve decrypted password"));
        }
    }
}
