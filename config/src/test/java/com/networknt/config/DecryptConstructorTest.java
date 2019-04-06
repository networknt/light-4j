package com.networknt.config;

import com.networknt.config.yml.DecryptConstructor;
import org.junit.Assert;
import org.junit.Test;

public class DecryptConstructorTest {
    @Test
    public void testConstructor() {
        DecryptConstructor constructor = new DecryptConstructor();
        Assert.assertNotNull(constructor);
    }
}
