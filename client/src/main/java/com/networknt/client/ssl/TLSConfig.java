package com.networknt.client.ssl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.utility.StringUtils;

/**
 * This class holds configuration values related to server identify check.
 * 
 * @author Daniel Zhao
 *
 */
public class TLSConfig {
	private static final Logger logger = LoggerFactory.getLogger(TLSConfig.class);
	private static final Map<String, TLSConfig> memcache = new ConcurrentHashMap<>();
	
    public static final String VERIFY_HOSTNAME="verifyHostname";
    public static final String TRUSTED_NAMES="trustedNames";
    public static final String DEFAULT_TRUSTED_NAME_GROUP_KEY="trustedNames.default";
    public static final String CONFIG_LEVEL_DELIMITER = "\\.";
    
    private final boolean checkServerIdentify;
    private final Set<String> trustedNameSet;
    private final EndpointIdentificationAlgorithm algorithm;
    
    private TLSConfig(boolean checkServerIdentify, Set<String> trustedNameSet) {
    	this.checkServerIdentify=checkServerIdentify;
    	this.trustedNameSet = Collections.unmodifiableSet(trustedNameSet);
    	this.algorithm = EndpointIdentificationAlgorithm.select(checkServerIdentify, trustedNameSet);
    }
    	
    public static TLSConfig create(final Map<String, Object> tlsMap) {
    	return create(tlsMap, DEFAULT_TRUSTED_NAME_GROUP_KEY);
    }
    
	public static TLSConfig create(final Map<String, Object> tlsMap, final String trustedNameGroupKey) {
    	return memcache.computeIfAbsent(trustedNameGroupKey, key -> new TLSConfig(Boolean.TRUE.equals(tlsMap.get(VERIFY_HOSTNAME)), 
    			resolveTrustedNames(tlsMap, key)));
    }
    
	@SuppressWarnings("unchecked")
	public static Set<String> resolveTrustedNames(Map<String, Object> tlsMap, String groupKey){
		if (StringUtils.isBlank(groupKey)) {
			return Collections.EMPTY_SET;
		}
		
		String[] levels = StringUtils.trimToEmpty(groupKey).split(TLSConfig.CONFIG_LEVEL_DELIMITER);
		
		if (levels.length<1) {//the groupKey has only '.'
			throw new InvalidGroupKeyException(groupKey);
		}
		
		Map<String, Object> innerMap = tlsMap;
		
		String level = null;
		
		for (int i=0; i<levels.length-1; ++i) {
			level = levels[i];
			innerMap = typeSafeGet(innerMap, level, Map.class, groupKey);
		}
		
		String leafLevel = levels[levels.length-1];
		String values = typeSafeGet(innerMap, leafLevel, String.class, groupKey);
		
		return resolveTrustedNames((String)values);
	}
	
	public static Set<String> resolveTrustedNames(String trustedNames){
		Set<String> nameSet = Arrays.stream(StringUtils.trimToEmpty(trustedNames).split(","))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toSet());
		
		if (logger.isDebugEnabled()) {
			logger.debug("trusted names {}", nameSet);
		}
		
		return nameSet;
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
    
    private static <T> T typeSafeGet(Map<String, Object> map, String key, Class<T> valueType, String groupKey) {
    	if (map.containsKey(key)) {
    		Object value = map.get(key);
    		
    		if (null==value || valueType.isAssignableFrom(value.getClass())) {
    			return valueType.cast(value);
    		}
    	}
    	
    	throw new InvalidGroupKeyException(groupKey);
    }
    
	@SuppressWarnings("serial")
	static class InvalidGroupKeyException extends IllegalArgumentException{
		InvalidGroupKeyException(String groupKey){
			super("Failed in resolving trustedNames. Invalid groupKey:" + groupKey);
		}
	}
}
