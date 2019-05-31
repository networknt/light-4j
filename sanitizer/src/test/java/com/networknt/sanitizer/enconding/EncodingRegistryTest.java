package com.networknt.sanitizer.enconding;

import com.networknt.sanitizer.FakeEncoding;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EncodingRegistryTest {

    @Before
    public void setUp() {
        EncodingRegistry.reset();
    }

    @Test
    public void shouldRegisterEncoding() {
        FakeEncoding encoding = new FakeEncoding();
        EncodingRegistry.registry(encoding);

        Assert.assertSame(encoding, EncodingRegistry.getEncoding());
    }

    @Test
    public void shouldHasEncodingIfWasRegisteredAEncoding() {
        FakeEncoding encoding = new FakeEncoding();
        EncodingRegistry.registry(encoding);

        Assert.assertTrue(EncodingRegistry.hasEncodingsRegistered());
    }


    @Test
    public void shouldHasNotEncodingIfWasRegisteredAEncoding() {
        Assert.assertFalse(EncodingRegistry.hasEncodingsRegistered());
    }
}