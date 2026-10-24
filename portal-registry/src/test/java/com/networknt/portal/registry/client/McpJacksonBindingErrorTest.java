package com.networknt.portal.registry.client;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class McpJacksonBindingErrorTest {
    @Test
    void invalidChaosConfigReturnsInvalidParamsAndPreservesConfig(@TempDir Path directory) throws Exception {
        // Optional chaos classes exist only in this loader, preserving the ordinary
        // classpath's unsupported-chaos tests and avoiding any real assault code.
        List<String> compilerArgs = new ArrayList<>(List.of("-proc:none", "-d", directory.toString()));
        for (String name : List.of("Exception", "Killapp", "Latency", "Memory")) {
            Path source = directory.resolve(name + "AssaultHandler.java");
            String body = name.equals("Latency")
                    ? "public static LatencyAssaultConfig config = new LatencyAssaultConfig();" : "";
            Files.writeString(source, "package com.networknt.chaos; public class " + name
                    + "AssaultHandler { " + body + " }");
            compilerArgs.add(source.toString());
        }
        Path config = directory.resolve("LatencyAssaultConfig.java");
        Files.writeString(config, "package com.networknt.chaos; public class LatencyAssaultConfig { public int delay = 3; }");
        compilerArgs.add(config.toString());
        assertNotNull(ToolProvider.getSystemJavaCompiler());
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                compilerArgs.toArray(String[]::new)));
        URL moduleClasses = McpHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{directory.toUri().toURL(), moduleClasses},
                getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith(McpHandler.class.getName()) || name.startsWith("com.networknt.chaos.")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> found = findLoadedClass(name);
                        if (found == null) found = findClass(name);
                        if (resolve) resolveClass(found);
                        return found;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> handler = loader.loadClass("com.networknt.chaos.LatencyAssaultHandler");
            Object original = handler.getField("config").get(null);
            PortalRegistryWebSocketClient client = mock(PortalRegistryWebSocketClient.class);
            Map<String, Object> envelope = Map.of("id", 704, "method", "tools/call", "params",
                    Map.of("name", "configure_chaos_monkey", "arguments",
                            Map.of("assaultType", "latency", "config", Map.of("delay", "not-a-number"))));
            loader.loadClass(McpHandler.class.getName()).getMethod("handle",
                    PortalRegistryWebSocketClient.class, Map.class).invoke(null, client, envelope);
            verify(client).sendError(eq(704), eq(-32602), anyString());
            verify(client, never()).sendError(eq(704), eq(-32603), anyString());
            assertSame(original, handler.getField("config").get(null));
        }
    }
}
