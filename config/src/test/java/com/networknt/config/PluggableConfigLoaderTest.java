package com.networknt.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore
public class PluggableConfigLoaderTest extends TestCase {

    private Config config = null;
    private final String homeDir = System.getProperty("user.home");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        config = Config.getInstance();
        config.clear();
        // write a config file into the user home directory.
        Map<String, Object> map = new HashMap<>();
        List<String> excludedList = Arrays.asList(new String[] {"openapi", "values", "status", "test_exclusion"});
        map.put("configLoaderClass", "com.networknt.config.TestConfigLoader");
        map.put("exclusionConfigFileList", excludedList);
        config.getMapper().writeValue(new File(homeDir + "/config.yml"), map);

        // Add home directory to the classpath of the system class loader.
        AppURLClassLoader classLoader = new AppURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        classLoader.addURL(new File(homeDir).toURI().toURL());
        config.setClassLoader(classLoader);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // Remove the test.json from home directory
        File test = new File(homeDir + "/config.yml");
        test.delete();
    }

    @Test
    public void testGetMapConfigByPluggableConfigLoader() {
        Map<String, Object> map = Config.getInstance().getJsonMapConfig("consul");
        Assert.assertEquals(map.get("ttlCheck"), false);
    }

    @Test
    public void testGetObjectConfigByPluggableConfigloader() {
        TestConsulConfig config = (TestConsulConfig)Config.getInstance().getJsonObjectConfig("consul", TestConsulConfig.class);
        Assert.assertEquals(config.isTtlCheck(), false);
    }
}
