package com.networknt.header;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class Jackson3HeaderCompatibilityTest {
    @Test
    void acceptsLegacyStringsAndRoundTripsStructuredHeaders() {
        var mapper = Config.getInstance().getMapper();
        HeaderRequestConfig request = mapper.readValue(
                "{\"remove\":\"one, two\",\"update\":\"key:value:with:colons,empty\"}", HeaderRequestConfig.class);
        assertEquals(List.of("one", "two"), request.getRemove());
        assertEquals(Map.of("key", "value:with:colons", "empty", ""), request.getUpdate());
        HeaderRequestConfig copy = mapper.readValue(mapper.writeValueAsString(request), HeaderRequestConfig.class);
        assertEquals(request.getRemove(), copy.getRemove());
        assertEquals(request.getUpdate(), copy.getUpdate());
    }

    @Test
    void acceptsStructuredResponseHeaderInput() {
        var mapper = Config.getInstance().getMapper();
        HeaderResponseConfig response = mapper.readValue(
                "{\"remove\":[\"one\",\"two\"],\"update\":{\"key\":\"value\"}}", HeaderResponseConfig.class);
        HeaderResponseConfig copy = mapper.readValue(mapper.writeValueAsString(response), HeaderResponseConfig.class);
        assertEquals(List.of("one", "two"), copy.getRemove());
        assertEquals(Map.of("key", "value"), copy.getUpdate());
    }
}
