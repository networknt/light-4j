package com.networknt.server;

import com.networknt.config.Config;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
 * Created by steve on 03/02/17.
 *
 * These test cases are commented out as they can only be executed one at a time
 * as they are depending on manipulating server.json in home directory. It executed
 * in mvn clean install, all tests will be started at the same time and they will
 * use the same server.json and cause "address in use error".
 */
public class HttpServerTest {
    static final Logger logger = LoggerFactory.getLogger(HttpServerTest.class);
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
        map.put("enableHttps", false);
        map.put("httpsPort", 8443);
        map.put("keystoreName", "tls/server.keystore");
        map.put("keystorePass", "secret");
        map.put("keyPass", "secret");
        map.put("serviceId", "com.networknt.apia-1.0.0");
        map.put("enableRegistry", false);
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
                Thread.sleep(10000);
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
    public void testHttpServer() {
        // send local host a request with port 8080
        String url = "http://localhost:8080";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("Hello World!", s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test(expected=HttpHostConnectException.class)
    public void testHttpsServer() throws Exception {
        // send local host a request with port 8443
        String url = "https://localhost:8443";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("Hello World!", s);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
