package com.networknt.security;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class Jackson3CertificateCompatibilityTest {
    @Test
    void acceptsLegacyCertificateStringAndRoundTripsMap() {
        var mapper = Config.getInstance().getMapper();
        SecurityJwtConfig jwt = mapper.readValue(
                "{\"certificate\":\"100 = primary.crt & 101=secondary.crt&empty\",\"clockSkewInSeconds\":60,\"keyResolver\":\"X509Certificate\"}", SecurityJwtConfig.class);
        assertEquals(Map.of("100", "primary.crt", "101", "secondary.crt", "empty", ""), jwt.getCertificate());
        SecurityJwtConfig copy = mapper.readValue(mapper.writeValueAsString(jwt), SecurityJwtConfig.class);
        assertEquals(jwt.getCertificate(), copy.getCertificate());
        assertEquals(60, copy.getClockSkewInSeconds());
        assertEquals("X509Certificate", copy.getKeyResolver());
    }

    @Test
    void acceptsCertificateObjectInput() {
        var mapper = Config.getInstance().getMapper();
        SecurityJwtConfig jwt = mapper.readValue("{\"certificate\":{\"100\":\"primary.crt\"}}", SecurityJwtConfig.class);
        assertEquals(Map.of("100", "primary.crt"), jwt.getCertificate());
    }
}
