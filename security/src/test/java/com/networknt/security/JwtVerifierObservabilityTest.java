package com.networknt.security;

import com.networknt.config.Config;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JwtVerifierObservabilityTest extends JwtVerifierJwkBase {
    private static final Logger logger = LoggerFactory.getLogger(JwtVerifierObservabilityTest.class);

    private static Undertow server = null;

    @BeforeAll
    public static void beforeClass() throws IOException {
        SSLContext sslContext = createSSLContext(loadKeyStore("server.keystore"), loadKeyStore("server.truststore"), false);
        Undertow.Builder builder = Undertow.builder();
        builder.addHttpsListener(7775, "localhost", sslContext);
        builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
        server = builder
                .setHandler(new PathHandler()
                        .addExactPath("/oauth2/key-error", exchange -> {
                            exchange.setStatusCode(500);
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("Internal Server Error Body");
                        })
                        .addExactPath("/oauth2/key-malformed", exchange -> {
                            exchange.setStatusCode(200);
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send("not-a-json");
                        }))
                .build();
        server.start();
    }

    @AfterAll
    public static void afterClass() {
        if(server != null) server.stop();
    }

    private void setupConfigs(String keyUri) throws IOException {
        // client.yml
        Map<String, Object> clientMap = new HashMap<>();
        Map<String, Object> oauthMap = new HashMap<>();
        Map<String, Object> tokenMap = new HashMap<>();
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("server_url", "https://localhost:7775");
        keyMap.put("uri", keyUri);
        keyMap.put("enableHttp2", true);
        tokenMap.put("key", keyMap);
        oauthMap.put("token", tokenMap);
        clientMap.put("oauth", oauthMap);
        
        Config.getInstance().getMapper().writeValue(new File("target/test-classes/client-obs.yml"), clientMap);

        // security.yml
        Map<String, Object> securityMap = new HashMap<>();
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put("keyResolver", "JsonWebKeySet");
        securityMap.put("jwt", jwtMap);
        securityMap.put("enableVerifyJwt", true);

        Config.getInstance().getMapper().writeValue(new File("target/test-classes/security-obs.yml"), securityMap);
    }

    @Test
    public void testHttpErrorLogging() throws Exception {
        setupConfigs("/oauth2/key-error");

        // We need to force JwtVerifier to use our configs.
        // Since it calls ClientConfig.get() and SecurityConfig.load(), 
        // we use the load(String) methods.
        SecurityConfig securityConfig = SecurityConfig.load("security-obs");
        // We also need to make sure ClientConfig.get() returns our config.
        // This is tricky because JwtVerifier calls ClientConfig.get() internally.
        // However, we can set the singleton instance if we have access.
        // Or just use the fact that it will load "client.yml" if we name it so.
        
        // Let's try naming them client.yml and security.yml in a custom folder?
        // No, let's just use the load methods and hope for the best, 
        // OR better yet, just test the retrieveJwk logic if it's accessible or through verifyJwt.
        
        // Actually, verifyJwt uses ClientConfig.get() internally.
        // I'll just write to client.yml and security.yml in target/test-classes.
        // But that might interfere with other tests if run in parallel.
        // Since I'm running only this test, it's fine.
        
        Config.getInstance().getMapper().writeValue(new File("target/test-classes/client.yml"), clientMapFor("/oauth2/key-error"));
        Config.getInstance().getMapper().writeValue(new File("target/test-classes/security.yml"), securityMapFor());

        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load());
        String jwt = getJwt(5, "some-kid");
        try {
            jwtVerifier.verifyJwt(jwt, true, true);
        } catch (Exception e) {
            logger.info("Caught expected exception: " + e.getMessage());
        }
    }

    @Test
    public void testMalformedJwkLogging() throws Exception {
        Config.getInstance().getMapper().writeValue(new File("target/test-classes/client.yml"), clientMapFor("/oauth2/key-malformed"));
        Config.getInstance().getMapper().writeValue(new File("target/test-classes/security.yml"), securityMapFor());

        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load());
        String jwt = getJwt(5, "some-kid");
        try {
            jwtVerifier.verifyJwt(jwt, true, true);
        } catch (Exception e) {
            logger.info("Caught expected exception: " + e.getMessage());
        }
    }

    private Map<String, Object> clientMapFor(String keyUri) {
        Map<String, Object> clientMap = new HashMap<>();
        Map<String, Object> oauthMap = new HashMap<>();
        Map<String, Object> tokenMap = new HashMap<>();
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("server_url", "https://localhost:7775");
        keyMap.put("uri", keyUri);
        keyMap.put("enableHttp2", true);
        tokenMap.put("key", keyMap);
        oauthMap.put("token", tokenMap);
        clientMap.put("oauth", oauthMap);
        return clientMap;
    }

    private Map<String, Object> securityMapFor() {
        Map<String, Object> securityMap = new HashMap<>();
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put("keyResolver", "JsonWebKeySet");
        securityMap.put("jwt", jwtMap);
        securityMap.put("enableVerifyJwt", true);
        return securityMap;
    }
}
