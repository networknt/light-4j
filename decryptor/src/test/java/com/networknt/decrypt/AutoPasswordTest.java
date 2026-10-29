package com.networknt.decrypt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AutoPasswordTest {
    @Test void missingPasswordFailsEvenUnderJUnit() {
        assumeTrue(System.getenv("light_4j_config_password") == null
                && System.getenv("LIGHT_4J_CONFIG_PASSWORD") == null);
        String lower = System.getProperty("light_4j_config_password");
        String upper = System.getProperty("LIGHT_4J_CONFIG_PASSWORD");
        try {
            System.clearProperty("light_4j_config_password");
            System.clearProperty("LIGHT_4J_CONFIG_PASSWORD");
            AutoAESSaltDecryptor decryptor = new AutoAESSaltDecryptor();
            assertThrows(IllegalStateException.class, decryptor::getPassword);
        } finally {
            if (lower == null) System.clearProperty("light_4j_config_password");
            else System.setProperty("light_4j_config_password", lower);
            if (upper == null) System.clearProperty("LIGHT_4J_CONFIG_PASSWORD");
            else System.setProperty("LIGHT_4J_CONFIG_PASSWORD", upper);
        }
    }
}
