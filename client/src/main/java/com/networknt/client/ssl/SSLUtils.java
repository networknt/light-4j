/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client.ssl;

import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLUtils {
	private static final Logger logger = LoggerFactory.getLogger(SSLUtils.class);
	
	public static void handleTrustValidationErrors(Throwable t) throws CertificateException{
		logger.error(t.getMessage(), t);
		
		if (t instanceof CertificateException) {
			throw (CertificateException)t;
		}
		
		throw new CertificateException(t);
	}
}
