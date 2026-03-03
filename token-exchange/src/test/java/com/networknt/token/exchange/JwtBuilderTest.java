package com.networknt.token.exchange;

import com.networknt.config.JsonMapper;
import com.networknt.token.exchange.schema.TtlUnit;
import com.networknt.token.exchange.schema.jwt.JwtSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Map;

/**
 * Unit tests for the JwtBuilder class.
 * Tests JWT construction including header, body, and signature generation.
 */
public class JwtBuilderTest {

    private JwtBuilder jwtBuilder;
    private TestTokenKeyStoreManager testKeyStoreManager;

    @BeforeEach
    public void setUp() {
        testKeyStoreManager = new TestTokenKeyStoreManager();
        jwtBuilder = new JwtBuilder(testKeyStoreManager);
    }

    @Test
    public void testBuildJwtStructure() {
        // Create a JWT schema with test data
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);

        // Verify JWT structure (header.payload.signature)
        Assertions.assertNotNull(jwt, "JWT should not be null");
        String[] parts = jwt.split("\\.");
        Assertions.assertEquals(3, parts.length, "JWT should have 3 parts");

        // Verify header can be decoded
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        Assertions.assertTrue(headerJson.contains("alg"), "Header should contain alg");
        Assertions.assertTrue(headerJson.contains("typ"), "Header should contain typ");

        // Verify body can be decoded
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assertions.assertTrue(bodyJson.contains("iss"), "Body should contain iss");
        Assertions.assertTrue(bodyJson.contains("sub"), "Body should contain sub");

        // Verify signature is not empty
        Assertions.assertFalse(parts[2].isEmpty(), "Signature should not be empty");
    }

    @Test
    public void testJwtHeaderContainsStaticFields() {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> header = JsonMapper.string2Map(headerJson);

        Assertions.assertEquals("RS256", header.get("alg"));
        Assertions.assertEquals("JWT", header.get("typ"));
    }

    @Test
    public void testJwtBodyContainsStaticFields() {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JsonMapper.string2Map(bodyJson);

        Assertions.assertEquals("test-issuer", body.get("iss"));
        Assertions.assertEquals("test-subject", body.get("sub"));
        Assertions.assertEquals("test-audience", body.get("aud"));
    }

    @Test
    public void testJwtBodyContainsExpiryField() {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JsonMapper.string2Map(bodyJson);

        // Verify exp field exists and is a reasonable future timestamp
        Assertions.assertTrue(body.containsKey("exp"), "Body should contain exp");
        Object expObj = body.get("exp");
        long exp;
        if (expObj instanceof Number) {
            exp = ((Number) expObj).longValue();
        } else {
            exp = Long.parseLong(expObj.toString());
        }
        long now = System.currentTimeMillis() / 1000;
        Assertions.assertTrue(exp > now, "Expiry should be in the future");
        // With 3600 second TTL, exp should be roughly now + 3600
        Assertions.assertTrue(exp <= now + 3700, "Expiry should be within expected range");
    }

    @Test
    public void testJwtBodyContainsCurrentTimeField() {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JsonMapper.string2Map(bodyJson);

        // Verify iat field exists and is approximately current time
        Assertions.assertTrue(body.containsKey("iat"), "Body should contain iat");
        Object iatObj = body.get("iat");
        long iat;
        if (iatObj instanceof Number) {
            iat = ((Number) iatObj).longValue();
        } else {
            iat = Long.parseLong(iatObj.toString());
        }
        long now = System.currentTimeMillis() / 1000;
        // Allow 5 seconds tolerance
        Assertions.assertTrue(Math.abs(iat - now) < 5, "iat should be approximately current time");
    }

    @Test
    public void testJwtSignatureVerification() throws Exception {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");

        // Get the public key for verification
        PublicKey publicKey = testKeyStoreManager.getPublicKey();

        // Verify signature
        String payload = parts[0] + "." + parts[1];
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(payload.getBytes(StandardCharsets.UTF_8));

        Assertions.assertTrue(verifier.verify(signatureBytes), "Signature should be valid");
    }

    @Test
    public void testBuildWithNullHeader() {
        JwtSchema jwtSchema = createTestJwtSchemaWithNullHeader();

        String jwt = jwtBuilder.build(jwtSchema);

        Assertions.assertNotNull(jwt, "JWT should not be null even with null header");
        String[] parts = jwt.split("\\.");
        Assertions.assertEquals(3, parts.length, "JWT should have 3 parts");

        // Header part should be empty JSON object encoded
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        Assertions.assertEquals("{}", headerJson);
    }

    @Test
    public void testBuildWithDifferentTtlUnits() {
        // Test with MINUTE unit
        JwtSchema jwtSchemaMinutes = createTestJwtSchemaWithTtlUnit(60, TtlUnit.MINUTE);

        String jwt = jwtBuilder.build(jwtSchemaMinutes);
        String[] parts = jwt.split("\\.");
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JsonMapper.string2Map(bodyJson);

        Object expObj = body.get("exp");
        long exp;
        if (expObj instanceof Number) {
            exp = ((Number) expObj).longValue();
        } else {
            exp = Long.parseLong(expObj.toString());
        }
        long now = System.currentTimeMillis() / 1000;
        // 60 minutes = 3600 seconds
        Assertions.assertTrue(exp > now + 3500 && exp < now + 3700, "Expiry should be approximately 60 minutes from now");
    }

    @Test
    public void testBuildWithInvalidAlgorithm() {
        JwtSchema jwtSchema = createTestJwtSchemaWithAlgorithm("INVALID_ALG");
        Assertions.assertThrows(IllegalArgumentException.class, () -> jwtBuilder.build(jwtSchema));
    }

    // Helper method to create a test JWT schema
    private JwtSchema createTestJwtSchema() {
        return createTestJwtSchemaWithTtlUnit(3600, TtlUnit.SECOND);
    }

    private JwtSchema createTestJwtSchemaWithTtlUnit(long ttl, TtlUnit unit) {
        // Use lowercase for ttlUnit as that's how Jackson deserializes the enum
        String unitStr = unit.name().toLowerCase();
        String json = String.format("""
                {
                    "jwtTtl": %d,
                    "ttlUnit": "%s",
                    "algorithm": "SHA256withRSA",
                    "keyStore": {
                        "name": "test-keystore",
                        "password": "password",
                        "alias": "test-alias",
                        "keyPass": "password"
                    },
                    "jwtHeader": {
                        "staticFields": {
                            "alg": "RS256",
                            "typ": "JWT"
                        }
                    },
                    "jwtBody": {
                        "staticFields": {
                            "iss": "test-issuer",
                            "sub": "test-subject",
                            "aud": "test-audience"
                        },
                        "expiryFields": ["exp"],
                        "currentTimeFields": ["iat"]
                    }
                }
                """, ttl, unitStr);
        return JsonMapper.fromJson(json, JwtSchema.class);
    }

    private JwtSchema createTestJwtSchemaWithNullHeader() {
        String json = """
                {
                    "jwtTtl": 3600,
                    "ttlUnit": "second",
                    "algorithm": "SHA256withRSA",
                    "keyStore": {
                        "name": "test-keystore",
                        "password": "password",
                        "alias": "test-alias",
                        "keyPass": "password"
                    },
                    "jwtBody": {
                        "staticFields": {
                            "iss": "test-issuer"
                        }
                    }
                }
                """;
        return JsonMapper.fromJson(json, JwtSchema.class);
    }

    private JwtSchema createTestJwtSchemaWithAlgorithm(String algorithm) {
        String json = String.format("""
                {
                    "jwtTtl": 3600,
                    "ttlUnit": "second",
                    "algorithm": "%s",
                    "keyStore": {
                        "name": "test-keystore",
                        "password": "password",
                        "alias": "test-alias",
                        "keyPass": "password"
                    },
                    "jwtHeader": {
                        "staticFields": {
                            "alg": "RS256",
                            "typ": "JWT"
                        }
                    },
                    "jwtBody": {
                        "staticFields": {
                            "iss": "test-issuer"
                        }
                    }
                }
                """, algorithm);
        return JsonMapper.fromJson(json, JwtSchema.class);
    }

    /**
     * Test implementation of TokenKeyStoreManager that uses an in-memory key pair.
     */
    private static class TestTokenKeyStoreManager extends TokenKeyStoreManager {
        private final KeyPair keyPair;

        public TestTokenKeyStoreManager() {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                this.keyPair = keyGen.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate test key pair", e);
            }
        }

        @Override
        public PrivateKey getPrivateKey(String keyStoreName, char[] keyStorePass, String keyAlias, char[] keyPass) {
            return keyPair.getPrivate();
        }

        public PublicKey getPublicKey() {
            return keyPair.getPublic();
        }
    }
}
