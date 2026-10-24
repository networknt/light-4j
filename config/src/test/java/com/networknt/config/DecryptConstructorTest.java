package com.networknt.config;

import com.networknt.config.yml.DecryptConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DecryptConstructorTest {
    @Test
    public void testConstructor() {
        DecryptConstructor constructor = DecryptConstructor.create();
        Assertions.assertNotNull(constructor);
    }
}
