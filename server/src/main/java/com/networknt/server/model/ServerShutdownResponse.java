package com.networknt.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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

	/**
	 * Default constructor for ServerShutdownResponse.
	 */
	public ServerShutdownResponse() {
	}

	/**
	 * Gets the shutdown time.
	 * @return Long shutdown time
	 */
	@JsonProperty("time")
	public java.lang.Long getTime() {
		return time;
	}

	/**
	 * Sets the shutdown time.
	 * @param time Long shutdown time
	 */
	public void setTime(java.lang.Long time) {
		this.time = time;
	}

	/**
	 * Gets the service ID.
	 * @return String service ID
	 */
	@JsonProperty("serviceId")
	public String getServiceId() {
		return serviceId;
	}

	/**
	 * Sets the service ID.
	 * @param serviceId String service ID
	 */
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * Gets the tag.
	 * @return String tag
	 */
	@JsonProperty("tag")
	public String getTag() {
		return tag;
	}

	/**
	 * Sets the tag.
	 * @param tag String tag
	 */
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
