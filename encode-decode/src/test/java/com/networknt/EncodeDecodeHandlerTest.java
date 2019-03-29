/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package com.networknt;

import com.networknt.decode.RequestDecodeHandler;
import com.networknt.encode.ResponseEncodeHandler;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class EncodeDecodeHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(EncodeDecodeHandlerTest.class);

    static Undertow server = null;
    static volatile String message;

    @BeforeClass
    public static void setUp() {
        if (server == null) {
            logger.info("starting server");

            // handler chain for the encode path.
            ResponseEncodeHandler encoder = new ResponseEncodeHandler();
            encoder.setNext(new HttpHandler() {
                @Override
                public void handleRequest(final HttpServerExchange exchange) throws Exception {
                    exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, message.length() + "");
                    exchange.getResponseSender().send(message, IoCallback.END_EXCHANGE);
                }
            });

            // handler chain for the decode path
            RequestDecodeHandler decoder = new RequestDecodeHandler();
            decoder.setNext(new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.getRequestReceiver().receiveFullBytes(new Receiver.FullBytesCallback() {
                        @Override
                        public void handle(HttpServerExchange exchange, byte[] message) {
                            exchange.getResponseSender().send(ByteBuffer.wrap(message));
                        }
                    });
                }
            });

            PathHandler pathHandler = new PathHandler();
            pathHandler.addPrefixPath("/encode", encoder);
            pathHandler.addPrefixPath("/decode", decoder);
            server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(pathHandler)
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
    }

    @Test
    public void testDeflateEncoding() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            sb.append("a message");
        }
        runTest(sb.toString(), "deflate");
        runTest("Hello World", "deflate");

    }

    @Test
    public void testGzipEncoding() throws Exception {
        runTest("Hello World", "gzip");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            sb.append("a message");
        }
        runTest(sb.toString(), "gzip");
    }


    public void runTest(final String theMessage, String encoding) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().disableContentCompression().build()){
            message = theMessage;
            HttpGet get = new HttpGet("http://localhost:8080/encode");
            get.setHeader(Headers.ACCEPT_ENCODING_STRING, encoding);
            HttpResponse result = client.execute(get);
            Assert.assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            Header[] header = result.getHeaders(Headers.CONTENT_ENCODING_STRING);
            Assert.assertEquals(encoding, header[0].getValue());
            byte[] body = HttpClientUtils.readRawResponse(result);

            HttpPost post = new HttpPost("http://localhost:8080/decode");
            post.setEntity(new ByteArrayEntity(body));
            post.addHeader(Headers.CONTENT_ENCODING_STRING, encoding);

            result = client.execute(post);
            Assert.assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            String sb = HttpClientUtils.readResponse(result);
            Assert.assertEquals(theMessage.length(), sb.length());
            Assert.assertEquals(theMessage, sb);
        }
    }
}
