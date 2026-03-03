package com.networknt.utility;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.networknt.utility.MapUtil.getValueIgnoreCase;

public class MapUtilTest {
    @Test
    public void testGetValueIgnoreCase() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("Key1", "Value1");
        hashMap.put("Key2", "Value2");

        // Get value from HashMap with case-insensitive key lookup
        String key = "key1";
        Optional<String> value = getValueIgnoreCase(hashMap, key);
        Assertions.assertTrue(value.isPresent());
    }

}
