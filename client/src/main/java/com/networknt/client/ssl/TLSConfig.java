package com.networknt.client.ssl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This class holds configuration values related to server identify check.
 * 
 * @author Daniel Zhao
 *
 */
public class TLSConfig {
    public static final String VERIFY_HOSTNAME="verifyHostname";
    public static final String TRUSTED_NAMES="trustedNames";
    
    private final boolean checkServerIdentify;
    private final Set<String> trustedNameSet;
    private final EndpointIdentificationAlgorithm algorithm;
    
    private TLSConfig(boolean checkServerIdentify, Set<String> trustedNameSet) {
    	this.checkServerIdentify=checkServerIdentify;
    	this.trustedNameSet = Collections.unmodifiableSet(trustedNameSet);
    	this.algorithm = EndpointIdentificationAlgorithm.select(checkServerIdentify, trustedNameSet);
    }
    	
    public static TLSConfig create(final Map<String, Object> tlsMap) {
    	return new TLSConfig(Boolean.TRUE.equals(tlsMap.get(VERIFY_HOSTNAME)), 
    			SSLUtils.resolveTrustedNames((String)tlsMap.get(TRUSTED_NAMES)));
    }
    
    public boolean getCheckServerIdentity() {
    	return checkServerIdentify;
    }
    
    public Set<String> getTrustedNameSet(){
    	return trustedNameSet;
    }
    
    public EndpointIdentificationAlgorithm getEndpointIdentificationAlgorithm() {
    	return algorithm;
    }
}
