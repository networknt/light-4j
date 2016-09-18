package com.networknt.validator;

import com.networknt.client.Client;
import com.networknt.server.Server;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by steve on 01/09/16.
 */
public class ValidatorHandlerTest {

    static final Logger logger = LoggerFactory.getLogger(ValidatorHandlerTest.class);

    static Server server = null;

    @Before
    public void setUp() {
        if(server == null) {
            logger.info("starting server");
            server.start();
        }
    }

    @After
    public void tearDown() throws Exception {
        if(server != null) {
            server.stop();
            logger.info("The server is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ;
            }
        }
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
}
