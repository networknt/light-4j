package com.networknt.acme.client;

import org.shredzone.acme4j.Session;

public class SessionFactory {
	public Session getLetsEncryptSession() {
		return new  Session("https://acme-staging-v02.api.letsencrypt.org/directory");
	}
	public Session getPebbleSession() {
		return new  Session("https://localhost:14000/dir");
	}
}
