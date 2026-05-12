package com.networknt.server;

import com.networknt.client.ssl.CompositeX509TrustManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultConfigLoaderTest {

    public static DefaultConfigLoader configLoader;

    @BeforeAll
    public static void beforeClass() {
        configLoader = new DefaultConfigLoader();
    }

    @Test
    public void testProcessNestedMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("test1",
                "\n" +
                "  languages:\n" +
                "    - Ruby\n" +
                "    - Perl\n" +
                "    - Python \n" +
                "  websites:\n" +
                "    YAML: yaml.org\n" +
                "    Ruby: ruby-lang.org\n" +
                "    Python: python.org\n" +
                "    Perl: use.perl.org");
        map.put("test2",
                "\n" +
                "  - aaaa\n" +
                "  - bbbb\n" +
                "  -\n" +
                "    id: 1\n" +
                "    name: company1\n" +
                "    price: 200W\n" +
                "  -\n" +
                "    id: 2\n" +
                "    name: company2\n" +
                "    price: 500W\n" +
                "  -\n" +
                "    -aaaa\n" +
                "    -bbbb\n" +
                "  - abc: abc\n" +
                "    ccc: ccc\n" +
                "    ddd: ddd");
        Method processNestedMapMethod = DefaultConfigLoader.class.getDeclaredMethod("processNestedMap", Map.class);
        processNestedMapMethod.setAccessible(true);
        processNestedMapMethod.invoke(configLoader, map);

        Map<String, Object> result = (Map)map.get("test1");
        List<String> result1 = (List)result.get("languages");
        Assertions.assertEquals("Ruby", result1.get(0));
        Map<String, String> result2 = (Map)result.get("websites");
        Assertions.assertEquals( "yaml.org", result2.get("YAML"));
        System.out.println(map);

    }

    @Test
    public void testNormalizeDownloadedFileName() {
        Assertions.assertEquals("client.keystore",
                DefaultConfigLoader.normalizeDownloadedFileName(
                        "certs.client.keystore",
                        DefaultConfigLoader.CONFIG_SERVER_CERTS_CONTEXT_ROOT));
        Assertions.assertEquals("logback.xml",
                DefaultConfigLoader.normalizeDownloadedFileName(
                        "files.logback.xml",
                        DefaultConfigLoader.CONFIG_SERVER_FILES_CONTEXT_ROOT));
        Assertions.assertEquals("certs.client.keystore",
                DefaultConfigLoader.normalizeDownloadedFileName(
                        "certs.client.keystore",
                        DefaultConfigLoader.CONFIG_SERVER_FILES_CONTEXT_ROOT));
        Assertions.assertEquals("server.truststore",
                DefaultConfigLoader.normalizeDownloadedFileName(
                        "server.truststore",
                        DefaultConfigLoader.CONFIG_SERVER_CERTS_CONTEXT_ROOT));
    }

    @Test
    public void testComposeBootstrapTrustManagersUsesCompositeWhenDefaultTrustIsAvailable() {
        TrustManager[] result = DefaultConfigLoader.composeBootstrapTrustManagers(
                new TrustManager[] { new TestTrustManager() },
                new TrustManager[] { new TestTrustManager() });

        Assertions.assertEquals(1, result.length);
        Assertions.assertTrue(result[0] instanceof CompositeX509TrustManager);
    }

    @Test
    public void testComposeBootstrapTrustManagersFallsBackWhenDefaultTrustIsMissing() {
        TrustManager[] bootstrapTrustManagers = new TrustManager[] { new TestTrustManager() };

        TrustManager[] result = DefaultConfigLoader.composeBootstrapTrustManagers(
                bootstrapTrustManagers,
                null);

        Assertions.assertSame(bootstrapTrustManagers, result);
    }

    @Test
    public void testLoadDefaultTrustManagersFallsBackWhenDefaultTrustStoreIsInvalid() {
        String trustStoreProperty = "javax.net.ssl.trustStore";
        String previousTrustStore = System.getProperty(trustStoreProperty);
        System.setProperty(trustStoreProperty, "target/missing-default-truststore.jks");
        try {
            Assertions.assertNull(DefaultConfigLoader.loadDefaultTrustManagers());
        } finally {
            if (previousTrustStore == null) {
                System.clearProperty(trustStoreProperty);
            } else {
                System.setProperty(trustStoreProperty, previousTrustStore);
            }
        }
    }

    private static class TestTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
