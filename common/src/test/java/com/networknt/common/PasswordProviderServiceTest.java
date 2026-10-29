package com.networknt.common;

import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.fixture.FixturePasswordDecryptor;
import com.networknt.test.ForkedStartup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PasswordProviderServiceTest {
    @TempDir Path directory;

    @Test
    void missingProviderInvalidatesFactoryInsteadOfReturningPartialBindings() throws Exception {
        check(AESSaltDecryptor.class.getName(), "invalid");
    }

    @Test
    void constructorFallbackAlsoInvalidatesFactory() throws Exception {
        check(PasswordProviderServiceProbe.ConstructorFallback.class.getName(), "invalid");
    }

    @Test
    void explicitProviderRegistersLaterSingletonsAndDecrypts() throws Exception {
        check(FixturePasswordDecryptor.class.getName(), "valid");
    }

    private void check(String provider, String mode) throws Exception {
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: com.networknt.decrypt.AutoAESSaltDecryptor\nallowDefaultValueEmpty: true\n");
        Files.writeString(directory.resolve("service.yml"), """
                singletons:
                - java.lang.Runnable:
                  - com.networknt.common.PasswordProviderServiceProbe$Before
                - com.networknt.decrypt.Decryptor:
                  - %s
                - java.lang.Object:
                  - com.networknt.common.PasswordProviderServiceProbe$After
                """.formatted(provider));
        var result = ForkedStartup.run(PasswordProviderServiceProbe.class, directory, mode);
        assertEquals(0, result.exitCode(), result.output());
        if (mode.equals("invalid")) assertTrue(result.output().contains("No master-password provider configured"));
    }
}
