/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.info;

import com.networknt.config.Config;
import com.networknt.status.Status;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 01/10/16.
 */
public class ServerInfoDisabledTest {
    static final Logger logger = LoggerFactory.getLogger(ServerInfoHandlerTest.class);

    static Undertow server = null;
    static String homeDir = System.getProperty("user.home");

    @BeforeClass
    public static void setUp() throws Exception {
        // inject in memory constructed info.json to homeDir as classpath
        Config.getInstance().clear();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("description", "server info config");
        map.put("enableServerInfo", false);
        Config.getInstance().getMapper().writeValue(new File(homeDir + "/info.json"), map);
        // Add home directory to the classpath of the system class loader.
        addURL(new File(homeDir).toURL());

        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
        // Remove the test.json from home directory
        File configFile = new File(homeDir + "/info.json");
        configFile.delete();
    }

    static void addURL(URL url) throws Exception {
        URLClassLoader classLoader
                = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class clazz = URLClassLoader.class;
        // Use reflection
        Method method = clazz.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(classLoader, new Object[]{url});
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing().add(Methods.GET, "/v1/server/info", new ServerInfoHandler());
    }

    @Test
    public void testServerInfo() throws Exception {
        String url = "http://localhost:8080/v1/server/info";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(404, statusCode);
            if(statusCode == 404) {
                Status status = Config.getInstance().getMapper().readValue(response.getEntity().getContent(), Status.class);
                Assert.assertNotNull(status);
                Assert.assertEquals("ERR10013", status.getCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
