package com.networknt.sanitizer.enconding;

import com.networknt.sanitizer.FakeEncoding;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EncodingRegistryTest {

    @Before
    public void setUp() {
        EncoderRegistry.reset();
    }

    @Test
    public void shouldRegisterEncoding() {
        FakeEncoding encoding = new FakeEncoding();
        EncoderRegistry.registry(encoding);

        Assert.assertSame(encoding, EncoderRegistry.getEncoding());
    }

    @Test
    public void shouldHasEncodingIfWasRegisteredAEncoding() {
        FakeEncoding encoding = new FakeEncoding();
        EncoderRegistry.registry(encoding);

        Assert.assertTrue(EncoderRegistry.hasEncodingsRegistered());
    }


    @Test
    public void shouldHasNotEncodingIfWasRegisteredAEncoding() {
        Assert.assertFalse(EncoderRegistry.hasEncodingsRegistered());
    }
}