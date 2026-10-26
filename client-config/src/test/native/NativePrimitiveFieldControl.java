import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.jackson.testing.PrimitiveBooleanCases;

/** Manual removal gate: this must pass before dropping the native setter workaround. */
public class NativePrimitiveFieldControl {
    public static class FieldOnly {
        @JsonProperty private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
    }

    public static void main(String[] args) {
        var totals = PrimitiveBooleanCases.verify(FieldOnly.class, "enabled",
                FieldOnly::isEnabled, true, false, false);
        System.out.println("PASS: field-only control, " + totals.cases()
                + " coercion cases and " + totals.defaults() + " defaults");
    }
}
