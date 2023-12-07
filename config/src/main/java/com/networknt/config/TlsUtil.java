package com.networknt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

public class TlsUtil {
    static final Logger logger = LoggerFactory.getLogger(TlsUtil.class);

    public static KeyStore loadKeyStore(final String name, final char[] password) {
        InputStream stream = null;
        try {
            stream = Config.getInstance().getInputStreamFromFile(name);
            if (stream == null) {
                String message = "Unable to load keystore '" + name + "', please provide the keystore matching the configuration in client.yml/server.yml to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            // try to load keystore as JKS
            try {
                KeyStore loadedKeystore = KeyStore.getInstance("JKS");
                loadedKeystore.load(stream, password);
                return loadedKeystore;
            } catch (Exception e) {
                // if JKS fails, attempt to load as PKCS12
                try {
                    stream.close();
                    stream = Config.getInstance().getInputStreamFromFile(name);
                    KeyStore loadedKeystore = KeyStore.getInstance("PKCS12");
                    loadedKeystore.load(stream, password);
                    return loadedKeystore;
                } catch (Exception e2) {
                    logger.error("Unable to load keystore " + name, e2);
                    throw new RuntimeException("Unable to load keystore " + name, e2);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to load stream for keystore " + name, e);
            throw new RuntimeException("Unable to load stream for keystore " + name, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.error("Unable to close stream for keystore " + name, e);
                }
            }
        }
    }
}
