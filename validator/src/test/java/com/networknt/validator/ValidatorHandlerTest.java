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

package com.networknt.validator;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.swagger.SwaggerHandler;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by steve on 01/09/16.
 */
public class ValidatorHandlerTest {

    static final Logger logger = LoggerFactory.getLogger(ValidatorHandlerTest.class);

    static Undertow server = null;

    @Before
    public void setUp() {
        if(server == null) {
            logger.info("starting server");

            HttpHandler handler = getPetStoreHandler();
            ValidatorHandler validatorHandler = new ValidatorHandler();
            validatorHandler.setNext(handler);
            handler = validatorHandler;

            BodyHandler bodyHandler = new BodyHandler();
            bodyHandler.setNext(handler);
            handler = bodyHandler;

            SwaggerHandler swaggerHandler = new SwaggerHandler();
            swaggerHandler.setNext(handler);
            handler = swaggerHandler;

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
            } catch (InterruptedException ignored) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
    }

    RoutingHandler getPetStoreHandler() {
        return Handlers.routing()


                .add(Methods.POST, "/v2/pet", exchange -> exchange.getResponseSender().send("addPet"))


                .add(Methods.DELETE, "/v2/pet/{petId}", exchange -> exchange.getResponseSender().send("deletePet"))


                .add(Methods.GET, "/v2/pet/findByStatus", exchange -> exchange.getResponseSender().send("findPetsByStatus"))


                .add(Methods.GET, "/v2/pet/findByTags", exchange -> exchange.getResponseSender().send("findPetsByTags"))


                .add(Methods.GET, "/v2/pet/{petId}", exchange -> exchange.getResponseSender().send("getPetById"))


                .add(Methods.PUT, "/v2/pet", exchange -> exchange.getResponseSender().send("updatePet"))


                .add(Methods.POST, "/v2/pet/{petId}", exchange -> exchange.getResponseSender().send("updatePetWithForm"))


                .add(Methods.POST, "/v2/pet/{petId}/uploadImage", exchange -> exchange.getResponseSender().send("uploadFile"))


                .add(Methods.DELETE, "/v2/store/order/{orderId}", exchange -> exchange.getResponseSender().send("deleteOrder"))


                .add(Methods.GET, "/v2/store/inventory", exchange -> exchange.getResponseSender().send("getInventory"))


                .add(Methods.GET, "/v2/store/order/{orderId}", exchange -> exchange.getResponseSender().send("getOrderById"))


                .add(Methods.POST, "/v2/store/order", exchange -> exchange.getResponseSender().send("placeOrder"))


                .add(Methods.POST, "/v2/user", exchange -> exchange.getResponseSender().send("createUser"))


                .add(Methods.POST, "/v2/user/createWithArray", exchange -> exchange.getResponseSender().send("createUsersWithArrayInput"))


                .add(Methods.POST, "/v2/user/createWithList", exchange -> exchange.getResponseSender().send("createUsersWithListInput"))


                .add(Methods.DELETE, "/v2/user/{username}", exchange -> exchange.getResponseSender().send("deleteUser"))


                .add(Methods.GET, "/v2/user/{username}", exchange -> exchange.getResponseSender().send("getUserByName"))


                .add(Methods.GET, "/v2/user/login", exchange -> exchange.getResponseSender().send("loginUser"))


                .add(Methods.GET, "/v2/user/logout", exchange -> exchange.getResponseSender().send("logoutUser"))


                .add(Methods.PUT, "/v2/user/{username}", exchange -> exchange.getResponseSender().send("updateUser"));
    }

    @Test
    public void testInvalidRequstPath() throws Exception {
        String url = "http://localhost:8080/api";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals(404, status);
            return null;
        };
        String responseBody = null;
        try {
            responseBody = client.execute(httpGet, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidMethod() throws Exception {
        String url = "http://localhost:8080/v2/pet";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals(405, status);
            return null;
        };
        String responseBody = null;
        try {
            responseBody = client.execute(httpGet, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidPost() throws Exception {
        String url = "http://localhost:8080/v2/pet";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");

        StringEntity entity = new StringEntity("{\"name\":\"Pinky\", \"photoUrl\": \"http://www.photo.com/1.jpg\"}");
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        Assert.assertEquals(400, response.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(response.getEntity());
        logger.debug("response body = " + body);
        Assert.assertNotEquals("addPet", body);

    }

    @Test
    public void testValidPost() throws Exception {
        String url = "http://localhost:8080/v2/pet";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");
        StringEntity entity = new StringEntity("{\"id\":0,\"category\":{\"id\":0,\"name\":\"string\"},\"name\":\"doggie\",\"photoUrls\":[\"string\"],\"tags\":[{\"id\":0,\"name\":\"string\"}],\"status\":\"available\"}");
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(response.getEntity());
        logger.debug("response body = " + body);
        Assert.assertEquals("addPet", body);

    }

    @Test
    public void testGetParam() throws Exception {
        String url = "http://localhost:8080/v2/pet/111";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(response.getEntity());
        logger.debug("response body = " + body);
        Assert.assertEquals("getPetById", body);
    }

    @Test
    public void testDeleteWithoutHeader() throws Exception {
        String url = "http://localhost:8080/v2/pet/111";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        HttpResponse response = client.execute(httpDelete);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(400, statusCode);
        if(statusCode == 400) {
            Status status = Config.getInstance().getMapper().readValue(response.getEntity().getContent(), Status.class);
            Assert.assertNotNull(status);
            Assert.assertEquals("ERR11017", status.getCode());
        }
    }

    @Test
    public void testDeleteWithHeader() throws Exception {
        String url = "http://localhost:8080/v2/pet/111";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        httpDelete.setHeader("api_key", "key");
        HttpResponse response = client.execute(httpDelete);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            String body = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("deletePet", body);
        }
    }

}
