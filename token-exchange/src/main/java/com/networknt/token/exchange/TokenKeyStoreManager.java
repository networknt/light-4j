package com.networknt.token.exchange;

import com.networknt.config.TlsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches loading keystores
 */
public class TokenKeyStoreManager {
    private static final Logger LOG = LoggerFactory.getLogger(TokenKeyStoreManager.class);
    private static final Map<String, KeyStore> KEY_STORE_MAP = new HashMap<>();

    private void addKeyStore(final String keyStoreName, final char[] keyStorePass) {
        if (keyStoreName == null || keyStorePass == null) {
            throw new IllegalArgumentException("keyStoreName and keyStorePass must not be null");
        }
        KEY_STORE_MAP.put(keyStoreName, TlsUtil.loadKeyStore(keyStoreName, keyStorePass));
    }

    /**
     * Retrieve the keystore. Load keystore first if not found within the cache.
     *
     * @param keyStoreName - name of the keystore being retrieved.
     * @param keyStorePass - char array pass for the keystore.
     * @return - returns a keystore.
     */
    private KeyStore getKeyStore(final String keyStoreName, final char[] keyStorePass) {
        if (!KEY_STORE_MAP.containsKey(keyStoreName)) {
            this.addKeyStore(keyStoreName, keyStorePass);
        }
        return KEY_STORE_MAP.get(keyStoreName);
    }

    public PrivateKey getPrivateKey(final String keyStoreName, final char[] keyStorePass, final String keyAlias, final char[] keyPass) {
        if (keyStoreName == null || keyStorePass == null || keyAlias == null || keyPass == null) {
            throw new IllegalArgumentException("keyStoreName, keyStorePass, keyAlias, and keyPass must not be null");
        }

        final var keystore = this.getKeyStore(keyStoreName, keyStorePass);
        /* load key from keystore based on provided alias */
        try {
            return (PrivateKey) keystore.getKey(keyAlias, keyPass);

        } catch (KeyStoreException e) {
            throw new RuntimeException("Keystore was not initialized correctly: " + e.getMessage(), e);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm for recovering key was not found: " + e.getMessage(), e);

        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException("Key could not be recovered: " + e.getMessage(), e);
        }
    }

    public TrustManager[] getTrustManagers(final String keyStoreName, final char[] keyStorePass, final String algorithm) {
        if (keyStoreName == null || keyStorePass == null) {
            throw new IllegalArgumentException("keyStoreName and keyStorePass must not be null");
        }

        final String alg = (algorithm == null) ? TrustManagerFactory.getDefaultAlgorithm() : algorithm;

        final var keyStore = this.getKeyStore(keyStoreName, keyStorePass);
        final TrustManagerFactory trustManagerFactory;

        try {
            trustManagerFactory = TrustManagerFactory.getInstance(alg);

        } catch (NoSuchAlgorithmException e) {
            LOG.error("No Provider supports a TrustManagerFactory implementation for the specified algorithm: '{}'", alg);
            return new TrustManager[0];
        }

        try {
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();

        } catch (KeyStoreException e) {
            LOG.error("Operation failed for '{}': {}", keyStoreName, e.getMessage());
            return new TrustManager[0];
        }
    }

    public KeyManager[] getKeyManagers(final String keyStoreName, final char[] keyStorePass, final char[] privateKeyPass, final String algorithm) {
        if (keyStoreName == null || keyStorePass == null || privateKeyPass == null) {
            throw new IllegalArgumentException("keyStoreName, keyStorePass, and privateKeyPass must not be null");
        }

        final String alg = (algorithm == null) ? KeyManagerFactory.getDefaultAlgorithm() : algorithm;

        final var keyStore = this.getKeyStore(keyStoreName, keyStorePass);
        final KeyManagerFactory keyManagerFactory;

        try {
            keyManagerFactory = KeyManagerFactory.getInstance(alg);

        } catch (NoSuchAlgorithmException e) {
            LOG.error("No Provider supports a KeyManagerFactory implementation for the specified algorithm: '{}'", alg);
            return new KeyManager[0];
        }

        try {
            keyManagerFactory.init(keyStore, privateKeyPass);
            return keyManagerFactory.getKeyManagers();

        } catch (KeyStoreException e) {
            LOG.error("Operation failed for '{}': {}", keyStoreName, e.getMessage());
            return new KeyManager[0];

        } catch (NoSuchAlgorithmException e) {
            LOG.error("Specified algorithm '{}' used for keystore '{}' is not available.", alg, keyStoreName);
            return new KeyManager[0];

        } catch (UnrecoverableKeyException e) {
            LOG.error("Key is not recoverable or the password used for '{}' was incorrect.", keyStoreName);
            return new KeyManager[0];
        }
    }
}
