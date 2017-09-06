package com.networknt.consul;

import java.util.List;
import java.util.stream.Collectors;

public class ConsulService {

	private String id;

	private String name;

	private List<String> tags;

	private String address;

	private Integer port;
	
	private long ttl;

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

	public void setPort(Integer port) {
		this.port = port;
	}

	public long getTtl() {
		return ttl;
	}

	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	@Override
	public String toString() {
		String s = tags.stream().map(Object::toString).collect(Collectors.joining("\",\""));
		return "{\"ID\":\"" + id +
				"\",\"Name\":\"" + name
				+ "\",\"Tags\":[\"" + s
				+ "\"],\"Address\":\"" + address
				+ "\",\"Port\":" + port
				+ ",\"Check\":{\"TTL\":\"" + ttl + "s\"}}";
	}

}
