package com.networknt.config.reload.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTest {

    static Undertow server = null;
    static final Logger logger = LoggerFactory.getLogger(ModuleRegistryGetHandlerTest.class);
    public static final String CONFIG_NAME = "body";

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");

            HttpHandler handler = getTestHandler();
            BodyHandler bodyHandler = new BodyHandler();
            bodyHandler.setNext(handler);
            handler = bodyHandler;
            server = Undertow.builder()
                    .addHttpListener(7080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
        ModuleRegistry.registerModule(BodyHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server.stop();
            server = null;
            logger.info("The server is stopped.");
        }
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/getmodules", new ModuleRegistryGetHandler())
                .add(Methods.POST, "/reloadconfig", new ConfigReloadHandler());
    }
}
