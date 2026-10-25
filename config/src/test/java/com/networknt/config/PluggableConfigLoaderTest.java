package com.networknt.config;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluggableConfigLoaderTest {

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path directory;
    private String previousConfigDirectory;
    private Class<? extends Config> configType;

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {

        // Initialize the shared singleton before installing a deliberately invalid fixture.
        configType = Config.getInstance().getClass();
        previousConfigDirectory = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
        System.setProperty(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {

        if (previousConfigDirectory == null) {
            System.clearProperty(Config.LIGHT_4J_CONFIG_DIR);
        } else {
            System.setProperty(Config.LIGHT_4J_CONFIG_DIR, previousConfigDirectory);
        }
    }

    @Test
    public void testGetMapConfigWithoutPluggableConfigLoader() throws Exception {
        // Write invalid config loader
        Map<String, Object> map = new HashMap<>();
        List<String> excludedList = Arrays.asList(new String[]{"openapi", "values", "status", "test_exclusion"});
        map.put("configLoaderClass", null);
        map.put("exclusionConfigFileList", excludedList);

        writeConfigFile(map, directory.toString());

        // Construct config instance
        Class<? extends Config> c = configType;
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

        writeConfigFile(map, directory.toString());

        // Construct config instance
        Class<? extends Config> c = configType;
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

        writeConfigFile(map, directory.toString());

        // Construct config instance
        Class<? extends Config> c = configType;
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

        writeConfigFile(map, directory.toString());

        // Construct config instance
        Class<? extends Config> c = configType;
        Constructor<? extends Config> ctr = c.getDeclaredConstructor();
        ctr.setAccessible(true);
        Config config = (Config) ctr.newInstance();

        RuntimeException failure = Assertions.assertThrows(RuntimeException.class,
                () -> config.getJsonMapConfig("consul"));
        Assertions.assertInstanceOf(ClassNotFoundException.class, failure.getCause());
    }

    private static void writeConfigFile(Map<String, Object> configMap, String path) {
        if (path.equals("")) {
            mapper.writeValue(new File(path), configMap);
        } else {
            new File(path).mkdirs();
            mapper.writeValue(new File(path + "/config.yml"), configMap);
        }
    }
}
