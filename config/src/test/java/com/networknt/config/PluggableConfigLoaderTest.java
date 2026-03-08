package com.networknt.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluggableConfigLoaderTest {

    private final String homeDir = System.getProperty("user.home");

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {

        System.setProperty(Config.LIGHT_4J_CONFIG_DIR, homeDir);
    }

    @AfterEach
    public void tearDown() throws Exception {

        // Remove the config.yml from home directory
        File test = new File(homeDir + "/config.yml");
        test.delete();
    }

    @Test
    public void testGetMapConfigWithoutPluggableConfigLoader() throws Exception {
        // Write invalid config loader
        Map<String, Object> map = new HashMap<>();
        List<String> excludedList = Arrays.asList(new String[]{"openapi", "values", "status", "test_exclusion"});
        map.put("configLoaderClass", null);
        map.put("exclusionConfigFileList", excludedList);

        writeConfigFile(map, homeDir);

        // Construct config instance
        Class<? extends Config> c = Config.getInstance().getClass();
        Constructor<? extends Config> ctr = c.getDeclaredConstructor();
        ctr.setAccessible(true);
        Config config = (Config) ctr.newInstance();

        Map<String, Object> mapConfig = config.getJsonMapConfig("consul");
        Assertions.assertEquals(mapConfig.get("ttlCheck"), true);
    }

    @Test
    public void testGetMapConfigByPluggableConfigLoader() throws Exception {
        // Write a config.yml
        Map<String, Object> map = new HashMap<>();
        List<String> excludedList = Arrays.asList(new String[]{"openapi", "values", "status", "test_exclusion"});
        map.put("configLoaderClass", "com.networknt.config.TestConfigLoader");
        map.put("exclusionConfigFileList", excludedList);

        writeConfigFile(map, homeDir);

        // Construct config instance
        Class<? extends Config> c = Config.getInstance().getClass();
        Constructor<? extends Config> ctr = c.getDeclaredConstructor();
        ctr.setAccessible(true);
        Config config = (Config) ctr.newInstance();

        Map<String, Object> mapConfig = config.getJsonMapConfig("consul");
        Assertions.assertEquals(mapConfig.get("ttlCheck"), false);
    }

    @Test
    public void testGetObjectConfigByPluggableConfigloader() throws Exception {
        // Write a config.yml
        Map<String, Object> map = new HashMap<>();
        List<String> excludedList = Arrays.asList(new String[]{"openapi", "values", "status", "test_exclusion"});
        map.put("configLoaderClass", "com.networknt.config.TestConfigLoader");
        map.put("exclusionConfigFileList", excludedList);

        writeConfigFile(map, homeDir);

        // Construct config instance
        Class<? extends Config> c = Config.getInstance().getClass();
        Constructor<? extends Config> ctr = c.getDeclaredConstructor();
        ctr.setAccessible(true);
        Config config = (Config) ctr.newInstance();

        TestConsulConfig objectConfig = (TestConsulConfig) config.getJsonObjectConfig("consul", TestConsulConfig.class);
        Assertions.assertEquals(objectConfig.isTtlCheck(), false);
    }

    @Test
    public void testInvalidConfigLoader() throws Exception {
        // Write a config.yml with invalid config loader
        Map<String, Object> map = new HashMap<>();
        List<String> excludedList = Arrays.asList(new String[]{"openapi", "values", "status", "test_exclusion"});
        map.put("configLoaderClass", "com.networknt.config.InvalidConfigLoader");
        map.put("exclusionConfigFileList", excludedList);

        writeConfigFile(map, homeDir);

        // Construct config instance
        Class<? extends Config> c = Config.getInstance().getClass();
        Constructor<? extends Config> ctr = c.getDeclaredConstructor();
        ctr.setAccessible(true);
        Config config = (Config) ctr.newInstance();

        try {
            Map<String, Object> mapConfig = config.getJsonMapConfig("consul");
            Assertions.fail();
        } catch (Exception e) {
        }
    }

    private static void writeConfigFile(Map<String, Object> configMap, String path) throws IOException {
        if (path.equals("")) {
            mapper.writeValue(new File(path), configMap);
        } else {
            new File(path).mkdirs();
            mapper.writeValue(new File(path + "/config.yml"), configMap);
        }
    }
}
