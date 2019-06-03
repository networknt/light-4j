package com.networknt.sanitizer;

import com.networknt.sanitizer.enconding.DefaultEncoding;
import com.networknt.sanitizer.enconding.EncoderRegistry;
import com.networknt.sanitizer.enconding.EncodingStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EncodingStrategyTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        EncoderRegistry.reset();
    }

    @Test
    public void shouldGetDefaultStratetyIfDoesNotFindForValue() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Encoding unknown: anyString");

        EncodingStrategy.of("anyString");
    }

    @Test
    public void shouldGetDefaultStrategyIfValueIsNull() {
        Assert.assertTrue(EncodingStrategy.of(null) instanceof DefaultEncoding);
    }

    @Test
    public void shouldGetDefaultStrategyIfValueIsDefault() {
        Assert.assertTrue(EncodingStrategy.of("default") instanceof DefaultEncoding);
    }

    @Test
    public void shouldGetEncodingFromRegistry() {
        FakeEncoding encoding = new FakeEncoding();
        EncoderRegistry.registry(encoding);

        Assert.assertSame(encoding, EncodingStrategy.of(encoding.getId()));
    }
}