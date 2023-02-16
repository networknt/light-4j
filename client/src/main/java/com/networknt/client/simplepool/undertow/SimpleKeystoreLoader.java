package com.networknt.client.simplepool.undertow;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Originally from com.networknt.utility.TlsUtil developed by Balloon
 */

public class SimpleKeystoreLoader
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleKeystoreLoader.class);

    public static KeyStore loadKeyStore(final String name, final char[] password) {
        try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
            if (stream == null) {
                String message =
                        "Unable to load keystore '"
                        + name
                        + "', please provide the keystore matching the configuration in client.yml/server.yml to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, password);
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load keystore " + name, e);
            throw new RuntimeException("Unable to load keystore " + name, e);
        }
    }

    public static KeyStore loadTrustStore(final String name, final char[] password) {
        try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
            if (stream == null) {
                String message = "Unable to load truststore '" + name + "', please provide the truststore matching the configuration in client.yml/server.yml to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, password);
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load truststore " + name, e);
            throw new RuntimeException("Unable to load truststore " + name, e);
        }
    }
}