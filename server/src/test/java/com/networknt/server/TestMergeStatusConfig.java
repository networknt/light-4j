package com.networknt.server;

import com.networknt.config.Config;
import com.networknt.status.Status;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TestMergeStatusConfig extends TestCase {

    private Config config = null;

    final String homeDir = System.getProperty("user.home");

    @Override
    public void setUp() throws Exception {
        super.setUp();

        config = Config.getInstance();
        setExternalizedConfigDir(homeDir);

        Map<String, Object> contents = new HashMap<>();
        contents.put("statusCode", 404);
        contents.put("code", "ERR99999");
        contents.put("message", "test");
        contents.put("description", "test");
        // write config file into externalized path
        writeConfigFile("ERR99999", contents);
    }

    @Override
    public void tearDown() throws Exception {
        File appStatus = new File(homeDir + "/app-status.yml");
        appStatus.delete();
    }

    @Test
    public void testAppStatus() {
        config.clear();
        // test default element without merging with app-status
        Status status0 = new Status("ERR10053");
        Assert.assertEquals(401, status0.getStatusCode());
        Server.mergeStatusConfig();
        Status status = new Status("ERR99999");
        Assert.assertEquals(404, status.getStatusCode());
        // test default element after merging
        Status status1 = new Status("ERR10053");
        Assert.assertEquals(401, status1.getStatusCode());
    }

    @Test
    public void testDuplicateStatus() {
        config.clear();
        try {
            Server.mergeStatusConfig();
            // second try to make sure duplication status appear
            Server.mergeStatusConfig();
            fail();
        } catch (RuntimeException expected) {
            // pass
        }
    }

    private void setExternalizedConfigDir(String externalizedDir) throws Exception {
        Field f1 = config.getClass().getDeclaredField("EXTERNALIZED_PROPERTY_DIR");
        f1.setAccessible(true);
        f1.set(config, externalizedDir.split(":"));
    }

    private void writeConfigFile(String key, Object value) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        config.getMapper().writeValue(new File(homeDir + "/app-status.yml"), map);
    }
}
