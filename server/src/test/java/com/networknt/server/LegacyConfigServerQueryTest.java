package com.networknt.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyConfigServerQueryTest {
    private Map<String, Object> previousConfig;
    private String previousEnv;
    private Method queryMethod;
    private final Logger logger = (Logger) LoggerFactory.getLogger(DefaultConfigLoader.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() throws Exception {
        previousConfig = DefaultConfigLoader.startupConfig;
        previousEnv = DefaultConfigLoader.lightEnv;
        DefaultConfigLoader.startupConfig = new HashMap<>();
        DefaultConfigLoader.lightEnv = "dev";
        queryMethod = DefaultConfigLoader.class.getDeclaredMethod("getConfigServerQueryParameters");
        queryMethod.setAccessible(true);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.WARN);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        DefaultConfigLoader.startupConfig = previousConfig;
        DefaultConfigLoader.lightEnv = previousEnv;
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @Test
    void forwardsAllLegacyParametersWithOrWithoutServiceId() throws Exception {
        DefaultConfigLoader.startupConfig.put("productId", "customer-product");
        DefaultConfigLoader.startupConfig.put("productVersion", "2.3.8");
        DefaultConfigLoader.startupConfig.put("apiId", "customer-api");
        DefaultConfigLoader.startupConfig.put("apiVersion", "1.0.0");
        String legacy = "&productId=customer-product&productVersion=2.3.8&apiId=customer-api&apiVersion=1.0.0&envTag=dev";
        assertEquals("?host=lightapi.net" + legacy, queryMethod.invoke(null));
        assertEquals(1, appender.list.size());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("deprecated"));
        assertTrue(appender.list.get(0).getFormattedMessage().contains("published configuration snapshots"));

        DefaultConfigLoader.startupConfig.put("serviceId", "customer-service");
        assertEquals("?host=lightapi.net&serviceId=customer-service" + legacy, queryMethod.invoke(null));
        // One warning per invocation (bootstrap/reload), not per legacy parameter.
        assertEquals(2, appender.list.size());
    }

    @Test
    void eachExplicitLegacyParameterSupportsHistoricalRequestShape() throws Exception {
        for (String name : new String[] {"productId", "productVersion", "apiId", "apiVersion"}) {
            DefaultConfigLoader.startupConfig.clear();
            DefaultConfigLoader.startupConfig.put(name, "custom-value");
            assertEquals("?host=lightapi.net&" + name + "=custom-value&envTag=dev", queryMethod.invoke(null));
        }
    }

    @Test
    void snapshotQueryOmitsNullLegacyParametersAndDoesNotWarn() throws Exception {
        DefaultConfigLoader.startupConfig.put("serviceId", " service ");
        DefaultConfigLoader.startupConfig.put("host", " example.com ");
        for (String name : new String[] {"productId", "productVersion", "apiId", "apiVersion"}) {
            DefaultConfigLoader.startupConfig.put(name, null);
        }
        DefaultConfigLoader.lightEnv = " dev ";
        assertEquals("?host=example.com&serviceId=service&envTag=dev", queryMethod.invoke(null));
        assertTrue(appender.list.isEmpty());
    }

    @Test
    void nullLegacyParametersDoNotBypassServiceIdValidation() {
        DefaultConfigLoader.startupConfig.put("productId", null);
        assertInvalid("startup.serviceId is required for Config Server");
        DefaultConfigLoader.startupConfig.put("serviceId", "  ");
        assertInvalid("startup.serviceId is required for Config Server");
        assertTrue(appender.list.isEmpty());
    }

    @Test
    void legacyLookupRetainsHostAndEnvironmentValidation() {
        DefaultConfigLoader.startupConfig.put("productId", "customer-product");
        DefaultConfigLoader.startupConfig.put("host", " ");
        assertInvalid("startup.host is required for Config Server");
        DefaultConfigLoader.startupConfig.remove("host");
        DefaultConfigLoader.lightEnv = " ";
        assertInvalid("startup.envTag is required for Config Server");
    }

    private void assertInvalid(String message) {
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> queryMethod.invoke(null));
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals(message, error.getCause().getMessage());
    }
}
