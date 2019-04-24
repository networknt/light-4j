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

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.networknt.client.ssl.TLSConfig.InvalidGroupKeyException;

public class TLSConfigTest {
	private static final Map<String, Object> tlsMap = new HashMap<>();
	private static final String LOCALHOST = "localhost";
	private static final String SOMEHOST = "somehost";
	private static final String EMPTY = "";
	
	
	@BeforeClass
	public static void fill_tls_map() {
		Map<String, Object> nameMap = new HashMap<>();
		Map<String, Object> groupMap = new HashMap<>();
		tlsMap.put("verifyHostname", Boolean.TRUE);
		tlsMap.put("trustedNames", nameMap);
		
		nameMap.put("local", LOCALHOST);
		nameMap.put("groups", groupMap);
		
		groupMap.put("group1", SOMEHOST);
		groupMap.put("group2", EMPTY);
		
	}

	@Test
	public void trusted_names_can_be_properly_resolved() {
		TLSConfig localConfig = TLSConfig.create(tlsMap, "trustedNames.local");
		
		assertTrue(localConfig.getTrustedNameSet().size()==1 && localConfig.getTrustedNameSet().contains(LOCALHOST));
		assertTrue(EndpointIdentificationAlgorithm.APIS == localConfig.getEndpointIdentificationAlgorithm());
		
		TLSConfig group1Config = TLSConfig.create(tlsMap, "trustedNames.groups.group1");
		assertTrue(group1Config.getTrustedNameSet().size()==1 && group1Config.getTrustedNameSet().contains(SOMEHOST));
		assertTrue(EndpointIdentificationAlgorithm.APIS == group1Config.getEndpointIdentificationAlgorithm());	
		
		TLSConfig group2Config = TLSConfig.create(tlsMap, "trustedNames.groups.group2");
		assertTrue(group2Config.getTrustedNameSet().isEmpty());
		assertTrue(EndpointIdentificationAlgorithm.HTTPS == group2Config.getEndpointIdentificationAlgorithm());	
	}
	
	@Test(expected=InvalidGroupKeyException.class)
	public void incomplete_group_key_throws_exception() {
		TLSConfig.create(tlsMap, "trustedNames.groups");
	}
	
	@Test(expected=InvalidGroupKeyException.class)
	public void nonexisting_group_key_throws_exception() {
		TLSConfig.create(tlsMap, "trustedNames.something");
	}	
	
	@Test
	public void trustedNames_is_optional() {
		Map<String, Object> map = new HashMap<>();
		map.put("verifyHostname", Boolean.TRUE);
		
		TLSConfig config = TLSConfig.create(map);
		
		assertTrue(config.getTrustedNameSet().isEmpty());
		assertTrue(EndpointIdentificationAlgorithm.HTTPS == config.getEndpointIdentificationAlgorithm());		
	}
	
	@Test
	public void trustedNames_is_not_resolved_if_not_needed() {
		Map<String, Object> map = new HashMap<>();
		map.put("verifyHostname", Boolean.FALSE);
		map.put("trustedNames", LOCALHOST);
		
		TLSConfig config = TLSConfig.create(map);
		
		assertTrue(config.getTrustedNameSet().isEmpty());
		assertTrue(null == config.getEndpointIdentificationAlgorithm());		
	}	
}
