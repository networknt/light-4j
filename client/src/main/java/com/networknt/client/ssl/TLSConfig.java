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
	// config item that specifies whether hostname verification should be enabled or not
    public static final String VERIFY_HOSTNAME="verifyHostname";
    // config item that specifies the default trustedNames group used to created default SSL context. 
    // This is used to create Http2Client.SSL if set.
    public static final String DEFAULT_GROUP_KEY="defaultGroupKey";
    //trusted hostnames, service names, service Ids, and so on.
    // Note: localhost and 127.0.0.1 are not trustable hostname/ip in general. So, these values should not be used as trusted names in production.
    public static final String TRUSTED_NAMES="trustedNames";
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
    	return create(tlsMap, null);
    }
    
	public static TLSConfig create(final Map<String, Object> tlsMap, final String trustedNameGroupKey) {
		String cacheKey = toCacheKey(Boolean.TRUE.equals(tlsMap.get(VERIFY_HOSTNAME)), trustedNameGroupKey);
		
    	return memcache.computeIfAbsent(cacheKey, key -> new TLSConfig(Boolean.TRUE.equals(tlsMap.get(VERIFY_HOSTNAME)), 
    			resolveTrustedNames(tlsMap, trustedNameGroupKey)));
    }
    
	@SuppressWarnings("unchecked")
	public static Set<String> resolveTrustedNames(Map<String, Object> tlsMap, String groupKey){
		if (StringUtils.isBlank(groupKey) // blank key (null, empty, or white spaces)
				|| !Boolean.TRUE.equals(tlsMap.get(VERIFY_HOSTNAME))) {// hostname verification is not enabled
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
    
    private static String toCacheKey(boolean verifyHostName, String key) {
    	return String.format("%b-%s", verifyHostName, StringUtils.trimToEmpty(key));
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
