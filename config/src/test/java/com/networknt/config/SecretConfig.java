package com.networknt.config;

import java.util.List;
import java.util.Map;

public class SecretConfig {
	private String serverKeystorePass;
	private String serverKeyPass;
	private String serverTruststorePass;
	private String clientKeystorePass;
	private String clientKeyPass;
	private String clientTruststorePass;
	private String authorizationCodeClientSecret;
	private String clientCredentialsClientSecret;
	private String keyClientSecret;
	private String emailPassword;
	private Map<String, String> testMap;
	private List<String> testArray;
	
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
	public String getEmailPassword() {
		return emailPassword;
	}
	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}
	public Map<String, String> getTestMap() {
		return testMap;
	}
	public void setTestMap(Map<String, String> testMap) {
		this.testMap = testMap;
	}
	public List<String> getTestArray() {
		return testArray;
	}
	public void setTestArray(List<String> testArray) {
		this.testArray = testArray;
	}
}
