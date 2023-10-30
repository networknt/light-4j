package com.networknt.config;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.networknt.*")
public class ConfigAbsolutePathTest {

    Config config;
    String pathTest;

    @Before
    public void setup() throws Exception {

        pathTest = System.getProperty("light-4j-config-dir");
        System.setProperty("light-4j-config-dir", ClassLoader.getSystemResource("config/absolutePath").getPath());
        config = Config.getInstance();

        FileInputStream mockInputStream = PowerMockito.mock(FileInputStream.class);
        PowerMockito.whenNew(FileInputStream.class)
                .withAnyArguments()
                .thenReturn(mockInputStream);

    }

    @After
    public void tearDown() {
        if(pathTest!=null) {
            System.setProperty("light-4j-config-dir", pathTest);
        } else {
            System.clearProperty("light-4j-config-dir");
        }
    }

    @Test
    public void testAbsolutePath() throws Exception {

        Method getConfigStreamMethod = config.getClass().getDeclaredMethod(
                "getConfigStream", String.class, String.class);
        getConfigStreamMethod.setAccessible(true);

        String pathString;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            pathString = "C:\\an\\absolute\\path\\file.yaml";
        } else {
            pathString = "/an/absolute/path/file.yaml";
        }
        getConfigStreamMethod.invoke(config, pathString, "");

        PowerMockito.verifyNew(FileInputStream.class).withArguments(pathString);
    }

    @Test
    public void testRelativePath() throws Exception {

        Method getConfigStreamMethod = config.getClass().getDeclaredMethod(
                "getConfigStream", String.class, String.class);
        getConfigStreamMethod.setAccessible(true);

        getConfigStreamMethod.invoke(config,"file.yaml", "");

        PowerMockito.verifyNew(FileInputStream.class).withArguments(System.getProperty("light-4j-config-dir") + "/file.yaml");
    }

}