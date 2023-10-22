package com.networknt.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.networknt.*")
public class ConfigAbsolutePathTest {

    Config config;

    @Before
    public void setup() throws Exception {

        System.setProperty("light-4j-config-dir", ClassLoader.getSystemResource("config/absolutePath").getPath());
        config = Config.getInstance();

        FileInputStream mockInputStream = PowerMockito.mock(FileInputStream.class);
        PowerMockito.whenNew(FileInputStream.class)
                .withAnyArguments()
                .thenReturn(mockInputStream);
    }

    @Test
    public void testAbsolutePath() throws Exception {

        Method getConfigStreamMethod = config.getClass().getDeclaredMethod(
                "getConfigStream", String.class, String.class);
        getConfigStreamMethod.setAccessible(true);

        getConfigStreamMethod.invoke(config,"/an/absolute/path/file.yaml", "");

        PowerMockito.verifyNew(FileInputStream.class).withArguments("/an/absolute/path/file.yaml");
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