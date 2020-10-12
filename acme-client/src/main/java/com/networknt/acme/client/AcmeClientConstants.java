package com.networknt.acme.client;

public class AcmeClientConstants {
	public static final String BASE_PATH = System.getProperty("user.home") + "/acme/";
	public static final String CERTIFICATE_PATH = BASE_PATH + "certificate.pem";
	public static final String CERTFICATE_SIGNING_REQUEST_PATH = BASE_PATH + "domain.csr";
	public static final String ACCOUNT_KEY_PATH = BASE_PATH + "account.key";
	public static final String DOMAIN_KEY_PATH = BASE_PATH + "domain.key";

}
