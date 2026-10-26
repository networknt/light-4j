import com.networknt.client.OAuthSignConfig;
import com.networknt.jackson.testing.PrimitiveBooleanCases;

/** Reflection metadata comes only from dependency jars. */
public class NativeOAuthSignConfigTest {
    public static void main(String[] args) {
        var totals = PrimitiveBooleanCases.verify(OAuthSignConfig.class, "enableHttp2",
                OAuthSignConfig::isEnableHttp2, true, false, false);
        System.out.println("PASS: native OAuth sign binding and serialization, "
                + totals.cases() + " coercion cases and " + totals.defaults() + " omitted defaults");
    }
}
