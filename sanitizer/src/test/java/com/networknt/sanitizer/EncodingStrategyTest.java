package com.networknt.sanitizer;

import com.networknt.sanitizer.enconding.DefaultEncoding;
import com.networknt.sanitizer.enconding.EncodingStrategy;
import org.junit.Assert;
import org.junit.Test;

public class EncodingStrategyTest {

    @Test
    public void shouldGetDefaultStratetyIfDoesNotFindForValue() {
        Assert.assertTrue(EncodingStrategy.of("anyString") instanceof DefaultEncoding);
    }
}