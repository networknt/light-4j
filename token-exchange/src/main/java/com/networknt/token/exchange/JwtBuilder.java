package com.networknt.token.exchange;

import com.networknt.config.JsonMapper;
import com.networknt.token.exchange.schema.TtlUnit;
import com.networknt.token.exchange.schema.cert.KeyStoreSchema;
import com.networknt.token.exchange.schema.jwt.JwtSchema;
import com.networknt.token.exchange.schema.jwt.JwtPartialSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;

/**
 * Handles JWT token construction for token requests.
 * JWT structure = { jwtHeader } . { jwtBody } . signed({ jwtHeader } . { jwtBody })
 */
public class JwtBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(JwtBuilder.class);

    private final TokenKeyStoreManager keyStoreManager;

    public JwtBuilder(final TokenKeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    /**
     * Builds a signed JWT token based on the provided schema.
     *
     * @param jwtSchema the JWT configuration schema
     * @return the constructed JWT string
     */
    public String build(final JwtSchema jwtSchema) {
        final var tokenBuilder = new StringBuilder();

        // Build header
        final var headerMap = buildJwtPart(jwtSchema.getJwtHeader(), jwtSchema.getJwtTtl(), jwtSchema.getTtlUnit());
        appendEncodedJson(tokenBuilder, headerMap);
        tokenBuilder.append(".");

        // Build body
        final var bodyMap = buildJwtPart(jwtSchema.getJwtBody(), jwtSchema.getJwtTtl(), jwtSchema.getTtlUnit());
        appendEncodedJson(tokenBuilder, bodyMap);

        if (LOG.isTraceEnabled()) {
            LOG.trace("JWT Header: {}", JsonMapper.toJson(headerMap));
            LOG.trace("JWT Body: {}", JsonMapper.toJson(bodyMap));
        }

        // Sign the token
        final var signature = sign(tokenBuilder.toString(), jwtSchema.getKeyStore(), jwtSchema.getAlgorithm());
        tokenBuilder.append(".").append(signature);

        return tokenBuilder.toString();
    }

    private Map<String, String> buildJwtPart(
            final JwtPartialSchema partSchema,
            final long ttl,
            final TtlUnit ttlUnit
    ) {
        if (partSchema == null) {
            return Map.of();
        }
        return partSchema.buildJwtMap(ttl, ttlUnit);
    }

    private void appendEncodedJson(final StringBuilder builder, final Map<String, String> map) {
        final var json = JsonMapper.toJson(map);
        builder.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(json.getBytes(StandardCharsets.UTF_8)));
    }

    private String sign(
            final String payload,
            final KeyStoreSchema keyStoreSchema,
            final String algorithm
    ) {
        final var privateKey = keyStoreManager.getPrivateKey(
                keyStoreSchema.getName(),
                keyStoreSchema.getPassword(),
                keyStoreSchema.getAlias(),
                keyStoreSchema.getKeyPass()
        );

        LOG.trace("Signing with algorithm: {}", algorithm);

        try {
            final var signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(signature.sign());

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algorithm '" + algorithm + "' is not available", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid key for algorithm '" + algorithm + "'", e);
        } catch (SignatureException e) {
            throw new IllegalArgumentException("Failed to sign JWT", e);
        }
    }
}
