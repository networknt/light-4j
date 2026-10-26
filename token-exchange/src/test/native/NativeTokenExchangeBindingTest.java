import com.networknt.config.Config;
import com.networknt.jackson.JacksonDefaults;
import com.networknt.token.exchange.TokenExchangeConfig;
import com.networknt.token.exchange.schema.RequestSchema;
import com.networknt.token.exchange.schema.UpdateSchema;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.exc.MismatchedInputException;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Predicate;

public class NativeTokenExchangeBindingTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("native-token-binding-");
        var configFile = directory.resolve("config.yml");
        // Synthetic fixture: no encrypted values, TLS, tokens or external services.
        Files.writeString(configFile, "decryptorClass: ''\n");
        System.setProperty("light-4j-config-dir", directory.toString());
        try {
            Config.getInstance().putInConfigCache("token-exchange", new HashMap<String, Object>());
            check(TokenExchangeConfig.class, "enabled", TokenExchangeConfig::isEnabled, false, false);
            check(TokenExchangeConfig.class, "enableHttp2", TokenExchangeConfig::isEnableHttp2, false, false);
            check(RequestSchema.class, "cacheHttpClient", RequestSchema::isCacheHttpClient, false, false);
            check(RequestSchema.class, "cacheSSLContext", RequestSchema::isCacheSSLContext, false, false);
            check(RequestSchema.class, "enableHttp2", RequestSchema::isEnableHttp2, false, false);
            check(UpdateSchema.class, "updateExpirationFromTtl", UpdateSchema::isUpdateExpirationFromTtl, true, true);
            var mapper = JacksonDefaults.mapper();
            if (mapper.readValue("{\"proxyPort\":8080}", RequestSchema.class).getProxyPort() != 8080
                    || mapper.readValue("{\"proxyPort\":8080}", TokenExchangeConfig.class).getProxyPort() != 8080) {
                throw new AssertionError("Primitive int binding changed");
            }
            System.out.println("PASS: token-exchange native binding, 156 boolean cases, 12 defaults and 2 primitive int fields");
        } finally {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(directory);
        }
    }

    private static <T> void check(Class<T> type, String property, Predicate<T> getter,
                                   boolean defaultValue, boolean skipNull) {
        for (boolean strict : new boolean[]{false, true}) {
            var mapper = JacksonDefaults.builder()
                    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, strict).build();
            if (getter.test(mapper.readValue("{}", type)) != defaultValue) {
                throw new AssertionError(type.getSimpleName() + ": default changed");
            }
            String[] inputs = {"true", "false", "null", "1", "0", "2", "\"true\"", "\"false\"", "\"yes\"", "[]", "\"\"", "\"1\"", "0.0"};
            Boolean nullValue = skipNull ? Boolean.valueOf(defaultValue) : (strict ? null : Boolean.FALSE);
            Boolean[] expected = {true, false, nullValue, true, false, true, true, false, null, null, strict ? null : false, null, null};
            for (int i = 0; i < inputs.length; i++) {
                String document = "{\"" + property + "\":" + inputs[i] + "}";
                if (expected[i] == null) {
                    try {
                        mapper.readValue(document, type);
                        throw new AssertionError("Expected rejection: " + property + "=" + inputs[i]);
                    } catch (MismatchedInputException expectedFailure) {
                        // Do not catch a native field-handle DatabindException.
                    }
                } else {
                    T value = mapper.readValue(document, type);
                    if (getter.test(value) != expected[i]) {
                        throw new AssertionError("Wrong binding: " + property + "=" + inputs[i]);
                    }
                    var output = mapper.readTree(mapper.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS).writeValueAsString(value));
                    if (!output.has(property) || output.get(property).booleanValue() != expected[i]) {
                        throw new AssertionError("Wrong serialization: " + property);
                    }
                }
            }
        }
    }
}
