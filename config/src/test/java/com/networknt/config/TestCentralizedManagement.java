package com.networknt.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nicholas Azar (@NicholasAzar)
 */
public class TestCentralizedManagement extends TestCase {

    @Test
    public void testMap_mergeApplied_mutatesInPlaceCorrectly() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.string}");
        CentralizedManagement.mergeMap(testMap);
        Assert.assertEquals("test", testMap.get("key").toString());
    }

    @Test
    public void testMap_mergeWhenFieldNotInValues_throwsException() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.somethingNotInValues}");
        try {
            CentralizedManagement.mergeMap(testMap);
            fail();
        } catch (ConfigException expected) {
            // pass
        }
    }
}
