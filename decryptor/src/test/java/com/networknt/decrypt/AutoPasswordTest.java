package com.networknt.decrypt;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AutoPasswordTest {
    private static final String LOWER = "light_4j_config_password";
    private static final String UPPER = "LIGHT_4J_CONFIG_PASSWORD";

    @Test
    void realPasswordLookupHonorsPrecedenceAndRejectsMissingPassword() throws Exception {
        // A child process lets this test control the environment without changing
        // the developer's environment or adding a seam to the production provider.
        for (String environment : new String[]{"both", "upper", "absent"}) {
            String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
            var builder = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                    "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    AutoPasswordTest.class.getName(), environment).inheritIO();
            builder.environment().remove(LOWER);
            builder.environment().remove(UPPER);
            if (!environment.equals("absent")) builder.environment().put(UPPER, "upper-environment");
            if (environment.equals("both")) builder.environment().put(LOWER, "lower-environment");
            Process child = builder.start();
            try {
                assertTrue(child.waitFor(30, TimeUnit.SECONDS), "Password lookup probe timed out");
                assertEquals(0, child.exitValue(), "Password lookup probe failed: " + environment);
            } finally {
                if (child.isAlive()) child.destroyForcibly();
            }
        }
    }

    public static void main(String[] args) {
        System.clearProperty(LOWER);
        System.clearProperty(UPPER);
        var decryptor = new AutoAESSaltDecryptor();
        if (args[0].equals("absent")) {
            // assertThrows invokes the real lookup from a JUnit stack frame.
            assertThrows(IllegalStateException.class, decryptor::getPassword);
        } else {
            assertArrayEquals((args[0].equals("both") ? "lower-environment" : "upper-environment")
                    .toCharArray(), decryptor.getPassword());
        }
        System.setProperty(UPPER, "upper-property");
        assertArrayEquals("upper-property".toCharArray(), decryptor.getPassword());
        System.setProperty(LOWER, "lower-property");
        assertArrayEquals("lower-property".toCharArray(), decryptor.getPassword());
        System.setProperty(LOWER, "");
        assertArrayEquals("upper-property".toCharArray(), decryptor.getPassword());
        System.setProperty(UPPER, "");
        if (args[0].equals("absent")) {
            assertThrows(IllegalStateException.class, decryptor::getPassword);
        } else {
            assertArrayEquals((args[0].equals("both") ? "lower-environment" : "upper-environment")
                    .toCharArray(), decryptor.getPassword());
        }
    }
}
