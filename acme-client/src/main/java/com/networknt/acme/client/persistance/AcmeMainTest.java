package com.networknt.acme.client.persistance;

import java.io.IOException;

import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.CertificateAuthority;

public class AcmeMainTest {
	public static void main(String[] args) throws AcmeException, InterruptedException, IOException {
		CertificateAuthority ca = new CertificateAuthority();
		ca.order((chain) -> System.out.println(chain));
	}
}
