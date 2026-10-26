import com.networknt.config.Config;
import com.networknt.jackson.JacksonDefaults;
import com.networknt.jackson.testing.PrimitiveBooleanCases;
import com.networknt.token.exchange.TokenExchangeConfig;
import com.networknt.token.exchange.schema.RequestSchema;
import com.networknt.token.exchange.schema.UpdateSchema;

import java.nio.file.Files;
import java.util.HashMap;

public class NativeTokenExchangeBindingTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("native-token-binding-");
        var configFile = directory.resolve("config.yml");
        // Synthetic fixture: no encrypted values, TLS, tokens or external services.
        Files.writeString(configFile, "decryptorClass: ''\n");
        System.setProperty("light-4j-config-dir", directory.toString());
        try {
            Config.getInstance().putInConfigCache("token-exchange", new HashMap<String, Object>());
            var totals = PrimitiveBooleanCases.verify(TokenExchangeConfig.class, "enabled", TokenExchangeConfig::isEnabled, false, false, true)
                    .plus(PrimitiveBooleanCases.verify(TokenExchangeConfig.class, "enableHttp2", TokenExchangeConfig::isEnableHttp2, false, false, true))
                    .plus(PrimitiveBooleanCases.verify(RequestSchema.class, "cacheHttpClient", RequestSchema::isCacheHttpClient, false, false, true))
                    .plus(PrimitiveBooleanCases.verify(RequestSchema.class, "cacheSSLContext", RequestSchema::isCacheSSLContext, false, false, true))
                    .plus(PrimitiveBooleanCases.verify(RequestSchema.class, "enableHttp2", RequestSchema::isEnableHttp2, false, false, true))
                    .plus(PrimitiveBooleanCases.verify(UpdateSchema.class, "updateExpirationFromTtl", UpdateSchema::isUpdateExpirationFromTtl, true, true, true));
            var mapper = JacksonDefaults.mapper();
            if (mapper.readValue("{\"proxyPort\":8080}", RequestSchema.class).getProxyPort() != 8080
                    || mapper.readValue("{\"proxyPort\":8080}", TokenExchangeConfig.class).getProxyPort() != 8080) {
                throw new AssertionError("Primitive int binding changed");
            }
            System.out.println("PASS: token-exchange native binding, " + totals.cases()
                    + " boolean cases, " + totals.defaults() + " defaults and both primitive int fields");
        } finally {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(directory);
        }
    }
}
