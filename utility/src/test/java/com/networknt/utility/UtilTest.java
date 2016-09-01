package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest {
    @Test
    public void testGetUUID() {
        String id1 = Util.getUUID();
        String id2 = Util.getUUID();
        Assert.assertNotEquals(id1, id2);
    }
}
