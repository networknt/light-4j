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

/**
 * Wrapper for Consul API responses, including metadata headers.
 *
 * @param <T> type of the response value
 */
public class ConsulResponse<T> {
	/**
	 * consul return result
	 */
	private T value;

	private Long consulIndex;

	private Boolean consulKnownLeader;

	private Long consulLastContact;

    /**
     * Default constructor for ConsulResponse.
     */
    public ConsulResponse() {
    }

    /**
     * Gets the response value.
     *
     * @return T response value
     */
	public T getValue() {
		return value;
	}

    /**
     * Sets the response value.
     *
     * @param value response value
     */
	public void setValue(T value) {
		this.value = value;
	}

    /**
     * Gets the Consul index for blocking queries.
     *
     * @return Long consul index
     */
	public Long getConsulIndex() {
		return consulIndex;
	}

    /**
     * Sets the Consul index.
     *
     * @param consulIndex consul index
     */
	public void setConsulIndex(Long consulIndex) {
		this.consulIndex = consulIndex;
	}

    /**
     * Checks if the leader is known.
     *
     * @return Boolean true if leader known
     */
	public Boolean getConsulKnownLeader() {
		return consulKnownLeader;
	}

    /**
     * Sets whether the leader is known.
     *
     * @param consulKnownLeader true if leader known
     */
	public void setConsulKnownLeader(Boolean consulKnownLeader) {
		this.consulKnownLeader = consulKnownLeader;
	}

    /**
     * Gets the last contact time with the leader.
     *
     * @return Long last contact time
     */
	public Long getConsulLastContact() {
		return consulLastContact;
	}

    /**
     * Sets the last contact time.
     *
     * @param consulLastContact last contact time
     */
	public void setConsulLastContact(Long consulLastContact) {
		this.consulLastContact = consulLastContact;
	}

}
