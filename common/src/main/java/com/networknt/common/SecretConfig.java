package com.networknt.common;

import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.Decryptor;

/**
 * Secret configuration class that maps to secret.yml
 *
 * @author Steve Hu
 */
public class SecretConfig {
    String serverKeystorePass;
    String serverKeyPass;
    String serverTruststorePass;
    String clientKeystorePass;
    String clientKeyPass;
    String clientTruststorePass;
    String authorizationCodeClientSecret;
    String clientCredentialsClientSecret;
    String keyClientSecret;
    String consulToken;

    public SecretConfig() {
    }

    public String getServerKeystorePass() {
        return serverKeystorePass;
    }

    public void setServerKeystorePass(String serverKeystorePass) {
        if(serverKeystorePass.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            serverKeystorePass = decryptor.decrypt(serverKeystorePass);
        }
        this.serverKeystorePass = serverKeystorePass;
    }

    public String getServerKeyPass() {
        return serverKeyPass;
    }

    public void setServerKeyPass(String serverKeyPass) {
        if(serverKeyPass.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            serverKeyPass = decryptor.decrypt(serverKeyPass);
        }
        this.serverKeyPass = serverKeyPass;
    }

    public String getServerTruststorePass() {
        return serverTruststorePass;
    }

    public void setServerTruststorePass(String serverTruststorePass) {
        if(serverTruststorePass.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            serverTruststorePass = decryptor.decrypt(serverTruststorePass);
        }
        this.serverTruststorePass = serverTruststorePass;
    }

    public String getClientKeystorePass() {
        return clientKeystorePass;
    }

    public void setClientKeystorePass(String clientKeystorePass) {
        if(clientKeystorePass.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            clientKeystorePass = decryptor.decrypt(clientKeystorePass);
        }
        this.clientKeystorePass = clientKeystorePass;
    }

    public String getClientKeyPass() {
        return clientKeyPass;
    }

    public void setClientKeyPass(String clientKeyPass) {
        if(clientKeyPass.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            clientKeyPass = decryptor.decrypt(clientKeyPass);
        }
        this.clientKeyPass = clientKeyPass;
    }

    public String getClientTruststorePass() {
        return clientTruststorePass;
    }

    public void setClientTruststorePass(String clientTruststorePass) {
        if(clientTruststorePass.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            clientTruststorePass = decryptor.decrypt(clientTruststorePass);
        }
        this.clientTruststorePass = clientTruststorePass;
    }

    public String getAuthorizationCodeClientSecret() {
        return authorizationCodeClientSecret;
    }

    public void setAuthorizationCodeClientSecret(String authorizationCodeClientSecret) {
        if(authorizationCodeClientSecret.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            authorizationCodeClientSecret = decryptor.decrypt(authorizationCodeClientSecret);
        }
        this.authorizationCodeClientSecret = authorizationCodeClientSecret;
    }

    public String getClientCredentialsClientSecret() {
        return clientCredentialsClientSecret;
    }

    public void setClientCredentialsClientSecret(String clientCredentialsClientSecret) {
        if(clientCredentialsClientSecret.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            clientCredentialsClientSecret = decryptor.decrypt(clientCredentialsClientSecret);
        }
        this.clientCredentialsClientSecret = clientCredentialsClientSecret;
    }

    public String getKeyClientSecret() {
        return keyClientSecret;
    }

    public void setKeyClientSecret(String keyClientSecret) {
        if(keyClientSecret.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            keyClientSecret = decryptor.decrypt(keyClientSecret);
        }
        this.keyClientSecret = keyClientSecret;
    }

    public String getConsulToken() {
        return consulToken;
    }

    public void setConsulToken(String consulToken) {
        if(consulToken.startsWith(Decryptor.CRYPT_PREFIX)) {
            Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
            if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
            consulToken = decryptor.decrypt(consulToken);
        }
        this.consulToken = consulToken;
    }
}

