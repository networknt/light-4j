package com.networknt.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigSystemPropTest {
	private static final String homeDir = System.getProperty("user.home");
	
	@BeforeClass
	public static void beforeClass() throws IOException {
		System.setProperty(Config.LIGHT_4J_CONFIG_DIR, homeDir);
		
        writeConfigFile("value", "default dir", homeDir);
        writeConfigFile("value", "externalized dir1", homeDir + "/dir1");
        writeConfigFile("value", "externalized dir2", homeDir + "/dir2");
	}
	
    @AfterClass
    public static void tearDown() throws Exception {
        File test1 = new File(homeDir + "/test.json");
        File test2 = new File(homeDir + "/dir1/test.json");
        File test3 = new File(homeDir + "/dir2/test.json");
        File testFolder1 = new File(homeDir + "/dir1");
        File testFolder2 = new File(homeDir + "/dir2");
        test1.delete();
        test2.delete();
        test3.delete();
        testFolder1.delete();
        testFolder2.delete();
    }	
	
	@Test
	public void test() throws Exception {
		Class<? extends Config> c = Config.getInstance().getClass();
		
		Constructor<? extends Config> ctr = c.getDeclaredConstructor();
		
		ctr.setAccessible(true);
		
		Config config = (Config) ctr.newInstance();
		
		Map<String, Object> map = config.getJsonMapConfigNoCache("test");
		Map<String, Object> map1 = config.getJsonMapConfigNoCache("test", "dir1");
		Map<String, Object> map2 = config.getJsonMapConfigNoCache("test", "dir2");
		
		assertEquals("default dir", map.get("value"));
		assertEquals("externalized dir1", map1.get("value"));
		assertEquals("externalized dir2", map2.get("value"));
	}
	
    private static void writeConfigFile(String key, String value, String path) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        if (path.equals("")) {
        	Config.getInstance().getMapper().writeValue(new File(path), map);
        } else {
            new File(path).mkdirs();
            Config.getInstance().getMapper().writeValue(new File(path + "/test.json"), map);
        }
    }
}
