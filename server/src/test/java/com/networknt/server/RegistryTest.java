package com.networknt.server;

import com.networknt.config.Config;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * This test depends on consul server so it should be disabled all the time unless
 * it is used. To start consul use the following command line.
 *
 * docker run -d -p 8400:8400 -p 8500:8500/tcp -p 8600:53/udp -e 'CONSUL_LOCAL_CONFIG={"acl_datacenter":"dc1","acl_default_policy":"deny","acl_down_policy":"extend-cache","acl_master_token":"the_one_ring","bootstrap_expect":1,"datacenter":"dc1","data_dir":"/usr/local/bin/consul.d/data","server":true}' consul agent -server -ui -bind=127.0.0.1 -client=0.0.0.0
 *
 * To access consul ui in order to check registered services, use this url: http://localhost:8500
 *
 * Created by steve on 29/01/17.
 */
public class RegistryTest {
    static final Logger logger = LoggerFactory.getLogger(RegistryTest.class);
    static final String homeDir = System.getProperty("user.home");

    static Server server = null;

    //@BeforeClass
    public static void setUp() throws Exception {
        // inject server config here.
        Config config = Config.getInstance();
        // write a config file into the user home directory
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("description", "server config");
        map.put("enableHttp", true);
        map.put("ip", "0.0.0.0");
        map.put("httpPort", 8080);
        map.put("enableHttps", true);
        map.put("httpsPort", 8443);
        map.put("keystoreName", "server.keystore");
        map.put("keystorePass", "secret");
        map.put("keyPass", "secret");
        map.put("serviceId", "com.networknt.apia-1.0.0");
        map.put("enableRegistry", true);
        addURL(new File(homeDir).toURI().toURL());
        config.getMapper().writeValue(new File(homeDir + "/server.json"), map);

        if (server == null) {
            logger.info("starting server");
            Server.start();
        }
    }

    //@AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            Server.stop();
            logger.info("The server is stopped.");
        }
    }

    public static void addURL(URL url) throws Exception {
        URLClassLoader classLoader
                = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class clazz = URLClassLoader.class;

        // Use reflection
        Method method = clazz.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(classLoader, new Object[]{url});
    }

    @Test
    public void testVoid() {

    }

    //@Test
    public void testServer() {
        // server cannot be started as there is no spi routing handler provider
        Assert.assertNull(server);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ignored) {

        }
    }

}
