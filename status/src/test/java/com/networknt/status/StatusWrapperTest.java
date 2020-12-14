package com.networknt.status;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SeparateClassloaderTestRunner.class)
public class StatusWrapperTest {
    private static Config config = null;
    private static final String homeDir = System.getProperty("user.home");

    @BeforeClass
    public static void setUp() throws Exception {
        config = Config.getInstance();

        // write a config file into the user home directory.
        List<String> implementationList = new ArrayList<>();
        implementationList.add("com.networknt.status.TestStatusWrapper");
        Map<String, List<String>> implementationMap = new HashMap<>();
        implementationMap.put("com.networknt.status.StatusWrapper", implementationList);
        List<Map<String, List<String>>> interfaceList = new ArrayList<>();
        interfaceList.add(implementationMap);
        Map<String, Object> singletons = new HashMap<>();
        singletons.put("singletons", interfaceList);
        config.getMapper().writeValue(new File(homeDir + "/service.json"), singletons);

        // Add home directory to the classpath of the system class loader.
        AppURLClassLoader classLoader = new AppURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        classLoader.addURL(new File(homeDir).toURI().toURL());
        config.setClassLoader(classLoader);
    }

    @AfterClass
    public static void tearDown() {
        // Remove the service.yml from home directory
        File test = new File(homeDir + "/service.json");
        test.delete();
    }

    @Test
    public void testStatusWrap() {
        StatusWrapper statusWrapper = SingletonServiceFactory.getBean(StatusWrapper.class);
        Status status = new Status("ERR10001");
        status = statusWrapper.wrap(status, null);
        Assert.assertEquals("{\"error\":{\"statusCode\":401,\"code\":\"ERR10001\",\"message\":\"AUTH_TOKEN_EXPIRED\",\"description\":\"Jwt token in authorization header expired\",\"customInfo\":\"custom_info\",\"severity\":\"ERROR\"}}", status.toString());
    }

    @Test
    public void testStatusWrapWithArgs() {
        StatusWrapper statusWrapper = SingletonServiceFactory.getBean(StatusWrapper.class);
        Status status = new Status("ERR11000","arg1", "arg2");
        status = statusWrapper.wrap(status, null);
        Assert.assertEquals("{\"error\":{\"statusCode\":400,\"code\":\"ERR11000\",\"message\":\"VALIDATOR_REQUEST_PARAMETER_QUERY_MISSING\",\"description\":\"Query parameter arg1 is required on path arg2 but not found in request.\",\"customInfo\":\"custom_info\",\"severity\":\"ERROR\"}}", status.toString());
    }
}
