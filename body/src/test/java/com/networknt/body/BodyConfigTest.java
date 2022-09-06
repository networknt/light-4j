package com.networknt.body;

import org.junit.Assert;
import org.junit.Test;

/**
 * This is a test case that show users how to config the appliedPathPrefixes in values.yml externalized
 * configuration file. There are three options for multiple strings.
 *
 */
public class BodyConfigTest {
    @Test
    public void canLoadPathPrefixes() {
        BodyConfig config = BodyConfig.load();
        Assert.assertEquals(2, config.getAppliedPathPrefixes().size());
    }
}
