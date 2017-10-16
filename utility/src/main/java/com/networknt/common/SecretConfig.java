package com.networknt.common;

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
        this.serverKeystorePass = serverKeystorePass;
    }

    public String getServerKeyPass() {
        return serverKeyPass;
    }

    public void setServerKeyPass(String serverKeyPass) {
        this.serverKeyPass = serverKeyPass;
    }

    public String getServerTruststorePass() {
        return serverTruststorePass;
    }

    public void setServerTruststorePass(String serverTruststorePass) {
        this.serverTruststorePass = serverTruststorePass;
    }

    public String getClientKeystorePass() {
        return clientKeystorePass;
    }

    public void setClientKeystorePass(String clientKeystorePass) {
        this.clientKeystorePass = clientKeystorePass;
    }

    public String getClientKeyPass() {
        return clientKeyPass;
    }

    public void setClientKeyPass(String clientKeyPass) {
        this.clientKeyPass = clientKeyPass;
    }

    public String getClientTruststorePass() {
        return clientTruststorePass;
    }

    public void setClientTruststorePass(String clientTruststorePass) {
        this.clientTruststorePass = clientTruststorePass;
    }

    public String getAuthorizationCodeClientSecret() {
        return authorizationCodeClientSecret;
    }

    public void setAuthorizationCodeClientSecret(String authorizationCodeClientSecret) {
        this.authorizationCodeClientSecret = authorizationCodeClientSecret;
    }

    public String getClientCredentialsClientSecret() {
        return clientCredentialsClientSecret;
    }

    public void setClientCredentialsClientSecret(String clientCredentialsClientSecret) {
        this.clientCredentialsClientSecret = clientCredentialsClientSecret;
    }

    public String getKeyClientSecret() {
        return keyClientSecret;
    }

    public void setKeyClientSecret(String keyClientSecret) {
        this.keyClientSecret = keyClientSecret;
    }

    public String getConsulToken() {
        return consulToken;
    }

    public void setConsulToken(String consulToken) {
        this.consulToken = consulToken;
    }
}

