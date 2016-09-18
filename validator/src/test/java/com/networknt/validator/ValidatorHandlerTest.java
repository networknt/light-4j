package com.networknt.validator;

import com.networknt.client.Client;
import com.networknt.security.JwtMockHandler;
import com.networknt.security.SwaggerHelper;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

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
            ((RoutingHandler)handler).add(Methods.POST, "/oauth/token", new JwtMockHandler());
            ValidatorHandler validatorHandler = new ValidatorHandler(handler);
            handler = validatorHandler;
            // TODO inject operation of /oauth/token to swagger in order to by pass validator
            Swagger swagger = SwaggerHelper.swagger;
            Path path = new Path();
            Operation post = new Operation();
            path.set("post", post);
            Map<String, Path> paths = swagger.getPaths();
            paths.put("/oauth/token", path);
            swagger.setPaths(paths);

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
    }

    RoutingHandler getPetStoreHandler() {
        RoutingHandler handler = Handlers.routing()


                .add(Methods.POST, "/pet", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("addPet");
                    }
                })


                .add(Methods.DELETE, "/pet/{petId}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("deletePet");
                    }
                })


                .add(Methods.GET, "/pet/findByStatus", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("findPetsByStatus");
                    }
                })


                .add(Methods.GET, "/pet/findByTags", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("findPetsByTags");
                    }
                })


                .add(Methods.GET, "/pet/{petId}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("getPetById");
                    }
                })


                .add(Methods.PUT, "/pet", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("updatePet");
                    }
                })


                .add(Methods.POST, "/pet/{petId}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("updatePetWithForm");
                    }
                })


                .add(Methods.POST, "/pet/{petId}/uploadImage", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("uploadFile");
                    }
                })


                .add(Methods.DELETE, "/store/order/{orderId}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("deleteOrder");
                    }
                })


                .add(Methods.GET, "/store/inventory", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("getInventory");
                    }
                })


                .add(Methods.GET, "/store/order/{orderId}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("getOrderById");
                    }
                })


                .add(Methods.POST, "/store/order", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("placeOrder");
                    }
                })


                .add(Methods.POST, "/user", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("createUser");
                    }
                })


                .add(Methods.POST, "/user/createWithArray", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("createUsersWithArrayInput");
                    }
                })


                .add(Methods.POST, "/user/createWithList", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("createUsersWithListInput");
                    }
                })


                .add(Methods.DELETE, "/user/{username}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("deleteUser");
                    }
                })


                .add(Methods.GET, "/user/{username}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("getUserByName");
                    }
                })


                .add(Methods.GET, "/user/login", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("loginUser");
                    }
                })


                .add(Methods.GET, "/user/logout", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("logoutUser");
                    }
                })


                .add(Methods.PUT, "/user/{username}", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("updateUser");
                    }
                })

                ;
        return handler;
    }

    @Test
    public void testInvalidRequstPath() throws Exception {
        String url = "http://localhost:8080/api";
        CloseableHttpClient client = Client.getInstance().getSyncClient();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                Assert.assertEquals(404, status);
                return null;
            }

        };
        String responseBody = null;
        try {
            Client.getInstance().addAuthorizationWithScopeToken(httpGet, "Bearer token");
            responseBody = client.execute(httpGet, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidMethod() throws Exception {
        String url = "http://localhost:8080/pet";
        CloseableHttpClient client = Client.getInstance().getSyncClient();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                Assert.assertEquals(405, status);
                return null;
            }
        };
        String responseBody = null;
        try {
            Client.getInstance().addAuthorizationWithScopeToken(httpGet, "Bearer token");
            responseBody = client.execute(httpGet, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidPost() throws Exception {
        String url = "http://localhost:8080/pet";
        CloseableHttpClient client = Client.getInstance().getSyncClient();
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity("{\"name\":\"Pinky\", \"photoUrl\": \"http://www.photo.com/1.jpg\"}");
        httpPost.setEntity(entity);
        Client.getInstance().addAuthorizationWithScopeToken(httpPost, "Bearer token");
        HttpResponse response = client.execute(httpPost);
        Assert.assertEquals(400, response.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(response.getEntity());
        logger.debug("response body = " + body);
        Assert.assertNotEquals("addPet", body);

    }

    @Test
    public void testValidPost() throws Exception {
        String url = "http://localhost:8080/pet";
        CloseableHttpClient client = Client.getInstance().getSyncClient();
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity("{\"id\":0,\"category\":{\"id\":0,\"name\":\"string\"},\"name\":\"doggie\",\"photoUrls\":[\"string\"],\"tags\":[{\"id\":0,\"name\":\"string\"}],\"status\":\"available\"}");
        httpPost.setEntity(entity);
        Client.getInstance().addAuthorizationWithScopeToken(httpPost, "Bearer token");
        HttpResponse response = client.execute(httpPost);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(response.getEntity());
        logger.debug("response body = " + body);
        Assert.assertEquals("addPet", body);

    }

}
