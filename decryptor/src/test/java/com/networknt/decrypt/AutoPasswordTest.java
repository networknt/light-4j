package com.networknt.decrypt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AutoPasswordTest {
    @Test void missingPasswordFailsEvenUnderJUnit() {
        AutoAESSaltDecryptor decryptor = new AutoAESSaltDecryptor() {
            @Override String getRuntimePassword() { return null; }
        };
        assertThrows(IllegalStateException.class, decryptor::getPassword);
    }
}
