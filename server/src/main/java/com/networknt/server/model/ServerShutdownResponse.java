package com.networknt.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * POJO class for response body of shutdown endpoint.
 *
 *
 */
public class ServerShutdownResponse {

	private java.lang.Long time;
	private String serviceId;
	private String tag;

	public ServerShutdownResponse() {
	}

	@JsonProperty("time")
	public java.lang.Long getTime() {
		return time;
	}

	public void setTime(java.lang.Long time) {
		this.time = time;
	}

	@JsonProperty("serviceId")
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	@JsonProperty("tag")
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ServerShutdownResponse ServerShutdownResponse = (ServerShutdownResponse) o;

		return Objects.equals(time, ServerShutdownResponse.time)
				&& Objects.equals(serviceId, ServerShutdownResponse.serviceId)
				&& Objects.equals(tag, ServerShutdownResponse.tag);
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, serviceId, tag);
	}

}
