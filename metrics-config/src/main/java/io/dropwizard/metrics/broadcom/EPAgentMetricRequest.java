package io.dropwizard.metrics.broadcom;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "host", "process", "agent", "metrics" })
public class EPAgentMetricRequest {

	@JsonProperty("host")
	private String host;
	@JsonProperty("process")
	private String process;
	@JsonProperty("agent")
	private String agent;
	@JsonProperty("metrics")
	private List<EPAgentMetric> metrics = null;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("host")
	public String getHost() {
		return host;
	}

	@JsonProperty("host")
	public void setHost(String host) {
		this.host = host;
	}

	@JsonProperty("process")
	public String getProcess() {
		return process;
	}

	@JsonProperty("process")
	public void setProcess(String process) {
		this.process = process;
	}

	@JsonProperty("agent")
	public String getAgent() {
		return agent;
	}

	@JsonProperty("agent")
	public void setAgent(String agent) {
		this.agent = agent;
	}

	@JsonProperty("metrics")
	public List<EPAgentMetric> getMetrics() {
		return metrics;
	}

	@JsonProperty("metrics")
	public void setMetrics(List<EPAgentMetric> metrics) {
		this.metrics = metrics;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

}
