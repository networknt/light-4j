import com.networknt.client.OAuthSignConfig;
import com.networknt.jackson.JacksonDefaults;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.exc.MismatchedInputException;

/** Runs as an executable; reflection metadata comes only from dependency jars. */
public class NativeOAuthSignConfigTest {
    public static void main(String[] args) {
        for (boolean strict : new boolean[]{false, true}) {
            var mapper = JacksonDefaults.builder()
                    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, strict).build();
            if (!mapper.readValue("{}", OAuthSignConfig.class).isEnableHttp2()) {
                throw new AssertionError("Omitted property lost enabled default");
            }
            String[] inputs = {"true", "false", "null", "1", "0", "2", "\"true\"", "\"false\"", "\"yes\"", "[]", "\"\"", "\"1\"", "0.0"};
            Boolean[] expected = {true, false, strict ? null : false, true, false, true, true, false, null, null, strict ? null : false, null, null};
            for (int i = 0; i < inputs.length; i++) {
                String json = "{\"enableHttp2\":" + inputs[i] + "}";
                if (expected[i] == null) {
                    try {
                        mapper.readValue(json, OAuthSignConfig.class);
                        throw new AssertionError("Expected rejection: " + inputs[i] + ", strict=" + strict);
                    } catch (MismatchedInputException expectedFailure) {
                        // Includes InvalidFormatException; other native failures must escape.
                    }
                } else {
                    var config = mapper.readValue(json, OAuthSignConfig.class);
                    if (config.isEnableHttp2() != expected[i]) {
                        throw new AssertionError("Wrong binding: " + inputs[i] + ", strict=" + strict);
                    }
                    var output = mapper.readTree(mapper.writeValueAsString(config));
                    if (!output.has("enableHttp2") || output.get("enableHttp2").booleanValue() != expected[i]) {
                        throw new AssertionError("Serialization changed: " + inputs[i]);
                    }
                }
            }
        }
        System.out.println("PASS: native OAuth sign binding and serialization, 26 coercion cases and both omitted defaults");
    }
}
