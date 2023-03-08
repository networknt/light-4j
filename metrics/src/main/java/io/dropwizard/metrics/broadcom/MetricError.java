package io.dropwizard.metrics.broadcom;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "metricName", "metricErrorCode", "metricErrorMsg", "metricErrorIndex" })
public class MetricError {

	@JsonProperty("metricName")
	private String metricName;
	@JsonProperty("metricErrorCode")
	private String metricErrorCode;
	@JsonProperty("metricErrorMsg")
	private String metricErrorMsg;
	@JsonProperty("metricErrorIndex")
	private String metricErrorIndex;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("metricName")
	public String getMetricName() {
		return metricName;
	}

	@JsonProperty("metricName")
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	@JsonProperty("metricErrorCode")
	public String getMetricErrorCode() {
		return metricErrorCode;
	}

	@JsonProperty("metricErrorCode")
	public void setMetricErrorCode(String metricErrorCode) {
		this.metricErrorCode = metricErrorCode;
	}

	@JsonProperty("metricErrorMsg")
	public String getMetricErrorMsg() {
		return metricErrorMsg;
	}

	@JsonProperty("metricErrorMsg")
	public void setMetricErrorMsg(String metricErrorMsg) {
		this.metricErrorMsg = metricErrorMsg;
	}

	@JsonProperty("metricErrorIndex")
	public String getMetricErrorIndex() {
		return metricErrorIndex;
	}

	@JsonProperty("metricErrorIndex")
	public void setMetricErrorIndex(String metricErrorIndex) {
		this.metricErrorIndex = metricErrorIndex;
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