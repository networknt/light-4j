package io.dropwizard.metrics.broadcom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "errorCode", "errorMessage", "invalidCount", "validCount", "metricErrors" })
public class EPAgentMetricResponse {

	@JsonProperty("errorCode")
	private String errorCode;
	@JsonProperty("errorMessage")
	private String errorMessage;
	@JsonProperty("invalidCount")
	private Integer invalidCount;
	@JsonProperty("validCount")
	private Integer validCount;
	@JsonProperty("metricErrors")
	private List<MetricError> metricErrors = null;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("errorCode")
	public String getErrorCode() {
		return errorCode;
	}

	@JsonProperty("errorCode")
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	@JsonProperty("errorMessage")
	public String getErrorMessage() {
		return errorMessage;
	}

	@JsonProperty("errorMessage")
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@JsonProperty("invalidCount")
	public Integer getInvalidCount() {
		return invalidCount;
	}

	@JsonProperty("invalidCount")
	public void setInvalidCount(Integer invalidCount) {
		this.invalidCount = invalidCount;
	}

	@JsonProperty("validCount")
	public Integer getValidCount() {
		return validCount;
	}

	@JsonProperty("validCount")
	public void setValidCount(Integer validCount) {
		this.validCount = validCount;
	}

	@JsonProperty("metricErrors")
	public List<MetricError> getMetricErrors() {
		return metricErrors;
	}

	@JsonProperty("metricErrors")
	public void setMetricErrors(List<MetricError> metricErrors) {
		this.metricErrors = metricErrors;
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
