package com.networknt.security;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class JwtVerifierJwkBase {
    static final Logger logger = LoggerFactory.getLogger(JwtVerifierJwkBase.class);
    public static String curr_kid = "7pGHLozGRXqv2g47T1HQag";
    public static String curr_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCHbnJ01Wm2twOVSKLE21x2yMNTJNPZmVqvEjJGmw5k1alD+ReOfvbeP1ifJ495CQLzwr4w9ZiFEcqn3D8fBm9a8aBA2TnCUZbc9s4CV1tb/n6PmCLM8Mzx3mZMBOFuOy97nBVbA3nW61CioU12QJDywnmYdtqJJNNQcgOFsquIiUWGw10tkQePTI7drCcfwpjHO9usbOoqx7hsM27tr51hyOYRuZnSoa4ths14c3i9z7VC6QWWecsP1c2ZuH0a6u/nZujH5LHZVxTURj89oVp7anYGBUFuosmFy/Xvq8ikoCcgWfJvVo20/X3A4HcltkOFeFd0b1CESNRjMs6JXkedAgMBAAECggEAMcLTKzp+7TOxjVhy9gHjp4F8wz/01y8RsuHstySh1UrsNp1/mkvsSRzdYx0WClLVUttrJnIW6E3xOFwklTG4GKJPT4SBRHTWCbplV1bhqpuHxRsRLlwL8ZLV43inm+kDOVfQQPC2A9HSfu7ll12B5LCwHOUOxvVQ7230/Vr4y+GacYHDO0aL7tWAC2fH8hXzvgSc+sosg/gIRro7aasP5GMuFZjtPANzwhovE8vq71ZQTCzEEm890NuzOOYLUCmkE+FDL6Fjg9lckcosmfPuBpqMjAMMAhIHLEwmWBX6najTcuxpzDT6H+4cmU8+TyX2OwBlyAWpFNTLp3ta05tAAQKBgQDRgSxGB83hx5IL1u1gvDsEfS2sKgRDE5ZEeNDOrxI+U6dhgKj7ae11as83AZnA+sAQrHPZowoRAnAlqNFTQKMLxQfocs2sl5pG5xkL7DrlteUtG6gDvjsbtL64wiy6WrfTJvcICiAw9skgSFX+ZTy9GhcvQVrrjrHrjMl2b+uHAQKBgQClfN7SdW9hxKbKzHzpJ4G74Vr0JqYmr2JPu5DezL/Mxnx+sKEA2ByqVAEO6pJKGR5GfwPh91BBc1sRA4PzWtLRR5Dve6dm1puhaXKeREwBgIoDnXvGDfsOnwHQcGJzSgqBmycTTDiBmjnYX8AkZkbHN5lIFriy7G063XsuGIh8nQKBgDpEVb7oXr9DlP/L99smnrdh5Tjzupm5Mdq7Sz+ge09wTqYUdWrvDAbS/OyMemmsk4xPmizWZm9SoUQoDoe7+1zDoK5qd39f7p13moSxX7QRgbqo7XKVDrVm8IBMKMpvfp6wQJYw0sErccaTt674Ewt43SfcYmAPILalQka5W+UBAoGAQpom83zf/vEuT6BNBWkpBXyFJo4HgLpFTuGmRIUTDE81+6cKpVRU9Rgp9N7jUX8aeDTWUzM90ZmjpQ1NJbv/7Mpownl5viHRMP1Ha/sAu/oHkbzn+6XUzOWhzUnt1YiPAep3p4SdmUuAzFx88ClZgwQVZLYAT8Jnk7FfygWFqOECgYBOox0DFatEqB/7MNMoLMZCacSrylZ1NYHJYAdWkxOvahrppAMbDVFDlwvH7i8gVvzcfFxQtOxSJBlUKlamDd5i76O2N+fIPO8P+iyqKz2Uh/emVwWCWlijSOnXvKRUOiujVufGP0OGxi1GKSUaIXnvMQqYF9M/Igi0BQiCn+pFzw==";
    public static String curr_pub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh25ydNVptrcDlUiixNtcdsjDUyTT2ZlarxIyRpsOZNWpQ/kXjn723j9YnyePeQkC88K+MPWYhRHKp9w/HwZvWvGgQNk5wlGW3PbOAldbW/5+j5gizPDM8d5mTAThbjsve5wVWwN51utQoqFNdkCQ8sJ5mHbaiSTTUHIDhbKriIlFhsNdLZEHj0yO3awnH8KYxzvbrGzqKse4bDNu7a+dYcjmEbmZ0qGuLYbNeHN4vc+1QukFlnnLD9XNmbh9Gurv52box+Sx2VcU1EY/PaFae2p2BgVBbqLJhcv176vIpKAnIFnyb1aNtP19wOB3JbZDhXhXdG9QhEjUYzLOiV5HnQIDAQAB";
    public static String jsonWebKeySetJsonCurr = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"7pGHLozGRXqv2g47T1HQag\",\"n\":\"h25ydNVptrcDlUiixNtcdsjDUyTT2ZlarxIyRpsOZNWpQ_kXjn723j9YnyePeQkC88K-MPWYhRHKp9w_HwZvWvGgQNk5wlGW3PbOAldbW_5-j5gizPDM8d5mTAThbjsve5wVWwN51utQoqFNdkCQ8sJ5mHbaiSTTUHIDhbKriIlFhsNdLZEHj0yO3awnH8KYxzvbrGzqKse4bDNu7a-dYcjmEbmZ0qGuLYbNeHN4vc-1QukFlnnLD9XNmbh9Gurv52box-Sx2VcU1EY_PaFae2p2BgVBbqLJhcv176vIpKAnIFnyb1aNtP19wOB3JbZDhXhXdG9QhEjUYzLOiV5HnQ\",\"e\":\"AQAB\"}]}";
    public static String jsonWebKeySetJsonLong = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"Tj_l_tIBTginOtQbL0Pv5w\",\"n\":\"0YRbWAb1FGDpPUUcrIpJC6BwlswlKMS-z2wMAobdo0BNxNa7hG_gIHVPkXu14Jfo1JhUhS4wES3DdY3a6olqPcRN1TCCUVHd-1TLd1BBS-yq9tdJ6HCewhe5fXonaRRKwutvoH7i_eR4m3fQ1GoVzVAA3IngpTr4ptnM3Ef3fj-5wZYmitzrRUyQtfARTl3qGaXP_g8pHFAP0zrNVvOnV-jcNMKm8YZNcgcs1SuLSFtUDXpf7Nr2_xOhiNM-biES6Dza1sMLrlxULFuctudO9lykB7yFh3LHMxtIZyIUHuy0RbjuOGC5PmDowLttZpPI_j4ynJHAaAWr8Ddz764WdQ\",\"e\":\"AQAB\"}]}";

    private static final char[] STORE_PASSWORD = "password".toCharArray();

    protected static KeyStore loadKeyStore(final String name) throws IOException {
        final InputStream stream = Config.getInstance().getInputStreamFromFile(name);
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, STORE_PASSWORD);

            return loadedKeystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    protected static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, boolean client) throws IOException {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, STORE_PASSWORD);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        }

        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        }

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

    protected static boolean isTokenExpired(String authorization) {
        boolean expired = false;
        String jwt = getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtConsumer consumer = new JwtConsumerBuilder()
                        .setSkipAllValidators()
                        .setDisableRequireSignature()
                        .setSkipSignatureVerification()
                        .build();

                JwtContext jwtContext = consumer.process(jwt);
                JwtClaims jwtClaims = jwtContext.getJwtClaims();

                try {
                    if ((NumericDate.now().getValue() - 60) >= jwtClaims.getExpirationTime().getValue()) {
                        expired = true;
                    }
                } catch (MalformedClaimException e) {
                    logger.error("MalformedClaimException:", e);
                }
            } catch(InvalidJwtException e) {
                e.printStackTrace();
            }
        }
        return expired;
    }

    protected static String getJwtFromAuthorization(String authorization) {
        String jwt = null;
        if(authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                String scheme = parts[0];
                String credentials = parts[1];
                Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(scheme).matches()) {
                    jwt = credentials;
                }
            }
        }
        return jwt;
    }

    public static String getJwt(JwtClaims claims, String kid) throws Exception {
        String jwt;
        PrivateKey privateKey = KeyUtil.deserializePrivateKey(curr_key, KeyUtil.RSA);

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS nested inside a JWE
        // So we first create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the sender's private key
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue(kid);

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        // Sign the JWS and produce the compact serialization, which will be the inner JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        jwt = jws.getCompactSerialization();
        return jwt;
    }

    private static PrivateKey getPrivateKey(String filename, String password, String key) {
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Http2Client.class.getResourceAsStream(filename),
                    password.toCharArray());

            privateKey = (PrivateKey) keystore.getKey(key,
                    password.toCharArray());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        if (privateKey == null) {
            logger.error("Failed to retrieve private key from keystore");
        }

        return privateKey;
    }

    protected static String getJwt(int expiredInSeconds, String kid) throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTime(NumericDate.fromMilliseconds(System.currentTimeMillis() + expiredInSeconds * 1000));
        return getJwt(claims, kid);
    }

    protected static JwtClaims getTestClaims() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("urn:com:networknt:oauth2:v1");
        claims.setAudience("urn:com.networknt");
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setClaim("version", "1.0");

        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    static Http2Client createClient() {
        return createClient(OptionMap.EMPTY);
    }

    static Http2Client createClient(final OptionMap options) {
        return Http2Client.getInstance();
    }

}
