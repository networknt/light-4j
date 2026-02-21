package com.networknt.utility;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class CollectionUtilsTest {

    @Test
    public void testMatchEndpointKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("/v1/cat/{petId}", "123");
        map.put("/v1/dog/{petId}/uploadImage", "456");
        map.put("/v1/fish/{petId}/uploadImage/{imageId}", "789");

        Assertions.assertEquals("123", CollectionUtils.matchEndpointKey("/v1/cat/123", map));
        Assertions.assertEquals("456", CollectionUtils.matchEndpointKey("/v1/dog/123/uploadImage", map));
        Assertions.assertEquals("789", CollectionUtils.matchEndpointKey("/v1/fish/123/uploadImage/456", map));
    }
}
