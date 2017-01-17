package com.networknt.consul;

public class ConsulResponse<T> {
	/**
	 * consul return result
	 */
	private T value;
	
	private Long consulIndex;
	
	private Boolean consulKnownLeader;
	
	private Long consulLastContact;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public Long getConsulIndex() {
		return consulIndex;
	}

	public void setConsulIndex(Long consulIndex) {
		this.consulIndex = consulIndex;
	}

	public Boolean getConsulKnownLeader() {
		return consulKnownLeader;
	}

	public void setConsulKnownLeader(Boolean consulKnownLeader) {
		this.consulKnownLeader = consulKnownLeader;
	}

	public Long getConsulLastContact() {
		return consulLastContact;
	}

	public void setConsulLastContact(Long consulLastContact) {
		this.consulLastContact = consulLastContact;
	}

	
}
