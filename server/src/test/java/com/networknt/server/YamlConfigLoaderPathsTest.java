package com.networknt.server;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.ConfigInjection;
import com.networknt.config.yml.YamlUtils;
import com.networknt.decrypt.Decryptor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.xnio.OptionMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class YamlConfigLoaderPathsTest {
    @TempDir Path directory;
    private HttpServer server;
    private HttpClient httpClient;
    private DefaultConfigLoader loader;
    private String responseBody;
    private String previousUri;
    private String previousTarget;
    private Map<String, Object> previousStartup;
    private Map<String, Object> previousDecrypted;
    private Map<String, Object> previousPlain;

    @BeforeEach
    void setUp() throws Exception {
        previousUri = DefaultConfigLoader.configServerUri;
        previousTarget = DefaultConfigLoader.targetConfigsDirectory;
        previousStartup = DefaultConfigLoader.startupConfig;
        previousDecrypted = ConfigInjection.getDecryptedValueMap();
        previousPlain = ConfigInjection.getUndecryptedValueMap();
        responseBody = "enabled: TRUE\nnumber: 0755\nlegacy: yes\n";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/yaml; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        DefaultConfigLoader.configServerUri = "http://127.0.0.1:" + server.getAddress().getPort();
        DefaultConfigLoader.targetConfigsDirectory = directory.toString();
        DefaultConfigLoader.startupConfig = Map.of();
        httpClient = HttpClient.newHttpClient();
        loader = new DefaultConfigLoader();
        Field client = DefaultConfigLoader.class.getDeclaredField("configClient");
        client.setAccessible(true);
        client.set(loader, httpClient);
    }

    @AfterEach
    void tearDown() {
        if (httpClient != null) httpClient.close();
        if (server != null) server.stop(0);
        DefaultConfigLoader.configServerUri = previousUri;
        DefaultConfigLoader.targetConfigsDirectory = previousTarget;
        DefaultConfigLoader.startupConfig = previousStartup;
        Config.getInstance().clear();
        Config.getNoneDecryptedInstance().clear();
        ConfigInjection.setDecryptedValueMap(previousDecrypted);
        ConfigInjection.setUndecryptedValueMap(previousPlain);
    }

    @Test
    void defaultLoaderParsesApplicationYamlResponse() throws Exception {
        Map<?, ?> values = (Map<?, ?>) invoke(loader, "getServiceConfigs", String.class, "/values.yml");
        assertEquals(Map.of("enabled", true, "number", 755, "legacy", "yes"), values);
    }

    @Test
    void defaultLoaderWritesValuesYamlThroughItsLoadPath() throws Exception {
        responseBody = "label: 'yes'\nenabled: true\nnested: [one, two]\n";
        assertEquals(true, invoke(loader, "loadConfigs", String.class, "?fixture=true"));
        Map<String, Object> values = YamlUtils.loadMap(YamlUtils.newLoad(), Files.readString(directory.resolve("values.yml")));
        assertEquals(Map.of("label", "yes", "enabled", true, "nested", java.util.List.of("one", "two")), values);
    }

    @Test
    void valuesWriterSupportsDatesAndInstantsAsIsoStrings() throws Exception {
        Instant instant = Instant.parse("2026-10-24T12:34:56.123456789Z");
        assertTrue(loader.writeValuesFile(Map.of("instant", instant, "date", Date.from(instant))));
        Map<String, Object> values = YamlUtils.loadMap(YamlUtils.newLoad(), Files.readString(directory.resolve("values.yml")));
        assertEquals(instant.toString(), values.get("instant"));
        assertEquals("2026-10-24T12:34:56.123Z", values.get("date"));
    }

    @Test
    void unsupportedDumpValuePreservesExistingFallbackFile() throws Exception {
        Path fallback = directory.resolve("values.yml");
        Files.writeString(fallback, "existing: retained\n");
        assertFalse(loader.writeValuesFile(Map.of("unsupported", new Object())));
        assertEquals("existing: retained\n", Files.readString(fallback));
    }

    @Test
    void ioFailureIsReportedWithoutInvalidatingCaches() throws Exception {
        Path notDirectory = directory.resolve("file");
        Files.writeString(notDirectory, "retained");
        DefaultConfigLoader.targetConfigsDirectory = notDirectory.toString();
        Map<String, Object> active = new java.util.HashMap<>(Map.of("sentinel", "retained"));
        ConfigInjection.setDecryptedValueMap(active);
        assertEquals(false, invoke(loader, "loadConfigs", String.class, "?fixture=true"));
        assertSame(active, ConfigInjection.getDecryptedValueMap());
        assertEquals("retained", Files.readString(notDirectory));
    }

    @Test
    void urlLoaderParsesMergesAndDecryptsYamlOverItsRealRequestPath() throws Exception {
        String oldUri = UrlConfigLoader.configServerUri;
        Map<String, Object> oldStartup = UrlConfigLoader.startupConfig;
        UrlConfigLoader urlLoader = new UrlConfigLoader();
        try {
            UrlConfigLoader.configServerUri = DefaultConfigLoader.configServerUri;
            UrlConfigLoader.startupConfig = Map.of("projectName", "test", "projectVersion", "1",
                    "serviceName", "fixture", "serviceVersion", "1");
            urlLoader.host = "127.0.0.1";
            urlLoader.connectionToken = UrlConfigLoader.client.borrow(URI.create(UrlConfigLoader.configServerUri),
                    Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            responseBody = "secret: CRYPT:fixture\nlegacy: yes\nenabled: TRUE\n";
            Config config = mock(Config.class);
            when(config.getYaml()).thenReturn(YamlUtils.newLoad(FixtureDecryptor.class.getName()));
            try (MockedStatic<Config> configs = mockStatic(Config.class)) {
                configs.when(Config::getInstance).thenReturn(config);
                Method load = UrlConfigLoader.class.getDeclaredMethod("loadConfigs");
                load.setAccessible(true);
                load.invoke(urlLoader);
                ArgumentCaptor<Object> values = ArgumentCaptor.forClass(Object.class);
                verify(config).putInConfigCache(eq("values"), values.capture());
                Map<?, ?> map = (Map<?, ?>) values.getValue();
                assertEquals("decoded-fixture", map.get("secret"));
                assertEquals("yes", map.get("legacy"));
                assertEquals(true, map.get("enabled"));
            }
        } finally {
            if (urlLoader.connectionToken != null) UrlConfigLoader.client.restore(urlLoader.connectionToken);
            UrlConfigLoader.configServerUri = oldUri;
            UrlConfigLoader.startupConfig = oldStartup;
        }
    }

    private static Object invoke(Object target, String name, Class<?> type, Object value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, type);
        method.setAccessible(true);
        return method.invoke(target, value);
    }

    public static final class FixtureDecryptor implements Decryptor {
        @Override
        public String decrypt(String input) {
            return "decoded-" + input.substring("CRYPT:".length());
        }
    }
}
