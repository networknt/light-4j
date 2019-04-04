/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.consul;

import com.networknt.config.Config;

import java.util.List;
import java.util.stream.Collectors;

import static com.networknt.consul.ConsulConstants.CONFIG_NAME;

public class ConsulService {
	static ConsulConfig config = (ConsulConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ConsulConfig.class);

	private String id;

	private String name;

	private List<String> tags;

	private String address;

	private Integer port;

	private String checkString;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) { this.port = port; }

	public ConsulService() {
		if(config.tcpCheck) {
			checkString = ",\"Check\":{\"ID\":\"check-%s\",\"DeregisterCriticalServiceAfter\":\"" + config.deregisterAfter + "\",\"TCP\":\"%s:%s\",\"Interval\":\"" + config.checkInterval + "\"}}";
		} else if(config.httpCheck) {
			checkString = ",\"Check\":{\"ID\":\"check-%s\",\"DeregisterCriticalServiceAfter\":\"" + config.deregisterAfter + "\",\"HTTP\":\"" + "https://%s:%s/health/%s" + "\",\"TLSSkipVerify\":true,\"Interval\":\"" + config.checkInterval + "\"}}";
		} else {
			checkString = ",\"Check\":{\"ID\":\"check-%s\",\"DeregisterCriticalServiceAfter\":\"" + config.deregisterAfter + "\",\"TTL\":\"" + config.checkInterval + "\"}}";
		}
	}

	/**
	 * Construct a register json payload. Note that deregister internal minimum is 1m.
	 *
	 * @return String
	 */
	@Override
	public String toString() {
		String s = tags.stream().map(Object::toString).collect(Collectors.joining("\",\""));
		return "{\"ID\":\"" + id +
				"\",\"Name\":\"" + name
				+ "\",\"Tags\":[\"" + s
				+ "\"],\"Address\":\"" + address
				+ "\",\"Port\":" + port
				+ String.format(checkString, id, address, port, name);
	}
}
