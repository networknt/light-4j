package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

public class ApiUtilTest {
    @Test
    public void testGetUUID() {
        String id1 = ApiUtil.getUUID();
        String id2 = ApiUtil.getUUID();
        Assert.assertNotEquals(id1, id2);
    }
}
