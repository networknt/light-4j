package com.networknt.token.exchange;

import com.networknt.config.JsonMapper;
import com.networknt.token.exchange.schema.TtlUnit;
import com.networknt.token.exchange.schema.jwt.JwtSchema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    @Before
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
        Assert.assertNotNull("JWT should not be null", jwt);
        String[] parts = jwt.split("\\.");
        Assert.assertEquals("JWT should have 3 parts", 3, parts.length);

        // Verify header can be decoded
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        Assert.assertTrue("Header should contain alg", headerJson.contains("alg"));
        Assert.assertTrue("Header should contain typ", headerJson.contains("typ"));

        // Verify body can be decoded
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assert.assertTrue("Body should contain iss", bodyJson.contains("iss"));
        Assert.assertTrue("Body should contain sub", bodyJson.contains("sub"));

        // Verify signature is not empty
        Assert.assertFalse("Signature should not be empty", parts[2].isEmpty());
    }

    @Test
    public void testJwtHeaderContainsStaticFields() {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> header = JsonMapper.string2Map(headerJson);

        Assert.assertEquals("RS256", header.get("alg"));
        Assert.assertEquals("JWT", header.get("typ"));
    }

    @Test
    public void testJwtBodyContainsStaticFields() {
        JwtSchema jwtSchema = createTestJwtSchema();

        String jwt = jwtBuilder.build(jwtSchema);
        String[] parts = jwt.split("\\.");
        String bodyJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JsonMapper.string2Map(bodyJson);

        Assert.assertEquals("test-issuer", body.get("iss"));
        Assert.assertEquals("test-subject", body.get("sub"));
        Assert.assertEquals("test-audience", body.get("aud"));
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
        Assert.assertTrue("Body should contain exp", body.containsKey("exp"));
        Object expObj = body.get("exp");
        long exp;
        if (expObj instanceof Number) {
            exp = ((Number) expObj).longValue();
        } else {
            exp = Long.parseLong(expObj.toString());
        }
        long now = System.currentTimeMillis() / 1000;
        Assert.assertTrue("Expiry should be in the future", exp > now);
        // With 3600 second TTL, exp should be roughly now + 3600
        Assert.assertTrue("Expiry should be within expected range", exp <= now + 3700);
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
        Assert.assertTrue("Body should contain iat", body.containsKey("iat"));
        Object iatObj = body.get("iat");
        long iat;
        if (iatObj instanceof Number) {
            iat = ((Number) iatObj).longValue();
        } else {
            iat = Long.parseLong(iatObj.toString());
        }
        long now = System.currentTimeMillis() / 1000;
        // Allow 5 seconds tolerance
        Assert.assertTrue("iat should be approximately current time", Math.abs(iat - now) < 5);
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

        Assert.assertTrue("Signature should be valid", verifier.verify(signatureBytes));
    }

    @Test
    public void testBuildWithNullHeader() {
        JwtSchema jwtSchema = createTestJwtSchemaWithNullHeader();

        String jwt = jwtBuilder.build(jwtSchema);

        Assert.assertNotNull("JWT should not be null even with null header", jwt);
        String[] parts = jwt.split("\\.");
        Assert.assertEquals("JWT should have 3 parts", 3, parts.length);

        // Header part should be empty JSON object encoded
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        Assert.assertEquals("{}", headerJson);
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
        Assert.assertTrue("Expiry should be approximately 60 minutes from now",
                exp > now + 3500 && exp < now + 3700);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithInvalidAlgorithm() {
        JwtSchema jwtSchema = createTestJwtSchemaWithAlgorithm("INVALID_ALG");
        jwtBuilder.build(jwtSchema);
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
