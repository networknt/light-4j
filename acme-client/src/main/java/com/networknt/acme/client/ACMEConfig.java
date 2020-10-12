package com.networknt.acme.client;

public class ACMEConfig {
	public static final String CONFIG_NAME = "acme";
	private String domain;
	private String session;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSession() {
		return session;
	}

	public void setSession(String session) {
		this.session = session;
	}

}
