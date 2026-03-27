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

/**
 * Model class representing a service in Consul.
 */
public class ConsulService {

	private String id;

	private String name;

	private List<String> tags;

	private String address;

	private Integer port;

	private String checkString;

    /**
     * Gets the service ID.
     *
     * @return String service ID
     */
	public String getId() {
		return id;
	}

    /**
     * Sets the service ID.
     *
     * @param id service ID
     */
	public void setId(String id) {
		this.id = id;
	}

    /**
     * Gets the service name.
     *
     * @return String service name
     */
	public String getName() {
		return name;
	}

    /**
     * Sets the service name.
     *
     * @param name service name
     */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * Gets the service tags.
     *
     * @return List of tags
     */
	public List<String> getTags() {
		return tags;
	}

    /**
     * Sets the service tags.
     *
     * @param tags list of tags
     */
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

    /**
     * Gets the service address.
     *
     * @return String service address
     */
	public String getAddress() {
		return address;
	}

    /**
     * Sets the service address.
     *
     * @param address service address
     */
	public void setAddress(String address) {
		this.address = address;
	}

    /**
     * Gets the service port.
     *
     * @return Integer service port
     */
	public Integer getPort() {
		return port;
	}

    /**
     * Sets the service port.
     *
     * @param port service port
     */
	public void setPort(Integer port) { this.port = port; }

    /**
     * Default constructor for ConsulService.
     * Initializes checkString based on ConsulConfig.
     */
	public ConsulService() {
                ConsulConfig config = ConsulConfig.load();
		if(config.tcpCheck) {
			checkString = ",\"Check\":{\"CheckID\":\"check-%s\",\"DeregisterCriticalServiceAfter\":\"" + config.deregisterAfter + "\",\"TCP\":\"%s:%s\",\"Interval\":\"" + config.checkInterval + "\"}}";
		} else if(config.httpCheck) {
			checkString = ",\"Check\":{\"CheckID\":\"check-%s\",\"DeregisterCriticalServiceAfter\":\"" + config.deregisterAfter + "\",\"HTTP\":\"" + "https://%s:%s/health/%s" + "\",\"TLSSkipVerify\":true,\"Interval\":\"" + config.checkInterval + "\"}}";
		} else {
			checkString = ",\"Check\":{\"CheckID\":\"check-%s\",\"DeregisterCriticalServiceAfter\":\"" + config.deregisterAfter + "\",\"TTL\":\"" + config.checkInterval + "\"}}";
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
