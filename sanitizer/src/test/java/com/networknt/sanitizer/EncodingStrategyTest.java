package com.networknt.sanitizer;

import org.junit.Assert;
import org.junit.Test;

public class EncodingStrategyTest {

    @Test
    public void shouldGetDefaultStratetyIfDoesNotFindForValue() {
        Assert.assertEquals(EncodingStrategy.DEFAULT, EncodingStrategy.of("anyString"));
    }
}