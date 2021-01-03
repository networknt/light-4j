package com.networknt.utility;

import org.junit.Test;

public class ByteUtilTest {
    @Test
    public void testNumeric() {
        System.out.println(ByteUtil.randomNumeric(100));
    }

    @Test
    public void testAlphabet() {
        System.out.println(ByteUtil.randomAlphabetic(100));
    }
}
