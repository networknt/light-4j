package com.networknt.acme.client.util;

import org.shredzone.acme4j.Session;

public class ACMEUtils {

	private static Session LETS_ENTRYPT_PRODUCTION_SESSION = new Session(
			"https://acme-v02.api.letsencrypt.org/directory");

	private static Session LETS_ENTRYPT_STAGING_SESSION = new Session(
			"https://acme-staging-v02.api.letsencrypt.org/directory");

	private static Session PEBBLE_SESSION = new Session("https://localhost:14000/dir");

	private ACMEUtils() {

	}

	public static Session getSession(String sessionType) {

		switch (sessionType) {

		case "PRODUCTION":
			return LETS_ENTRYPT_PRODUCTION_SESSION;

		case "STAGING":
			return LETS_ENTRYPT_STAGING_SESSION;

		case "PEBBLE":
			return PEBBLE_SESSION;

		default:
			return LETS_ENTRYPT_PRODUCTION_SESSION;

		}
	}
}
