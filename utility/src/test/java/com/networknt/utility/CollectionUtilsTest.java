package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CollectionUtilsTest {

    @Test
    public void testMatchEndpointKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("/v1/cat/{petId}", "123");
        map.put("/v1/dog/{petId}/uploadImage", "456");
        map.put("/v1/fish/{petId}/uploadImage/{imageId}", "789");

        Assert.assertEquals("123", CollectionUtils.matchEndpointKey("/v1/cat/123", map));
        Assert.assertEquals("456", CollectionUtils.matchEndpointKey("/v1/dog/123/uploadImage", map));
        Assert.assertEquals("789", CollectionUtils.matchEndpointKey("/v1/fish/123/uploadImage/456", map));
    }
}
