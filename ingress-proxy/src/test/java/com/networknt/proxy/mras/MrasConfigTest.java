package com.networknt.proxy.mras;

import org.junit.Assert;
import org.junit.Test;

public class MrasConfigTest {
    @Test
    public void testConfigLoad() {
        MrasConfig config = MrasConfig.load();
        Assert.assertEquals(2, config.appliedPathPrefixes.size());
    }
}
