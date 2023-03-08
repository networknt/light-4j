package com.networknt.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class APMMetricsConfig {
    boolean enabled;
    int reportInMinutes;
    String apmProtocol;
    String apmHost;
    String apmEPAgentPath;
    String serviceId;
    int apmPort;

    @JsonIgnore
    String description;

    public APMMetricsConfig() {
    }

    public APMMetricsConfig(boolean enabled, int reportInMinutes, String apmProtocol, String apmHost, String apmEPAgentPath, String serviceId, int apmPort) {
        this.enabled = enabled;
        this.reportInMinutes = reportInMinutes;
        this.apmProtocol = apmProtocol;
        this.apmHost = apmHost;
        this.apmEPAgentPath = apmEPAgentPath;
        this.serviceId = serviceId;
        this.apmPort = apmPort;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApmProtocol() {
		return apmProtocol;
	}

	public void setApmProtocol(String apmProtocol) {
		this.apmProtocol = apmProtocol;
	}

	public String getApmHost() {
		return apmHost;
	}

	public void setApmHost(String apmHost) {
		this.apmHost = apmHost;
	}

    public String getApmEPAgentPath() {
		return apmEPAgentPath;
	}

	public void setApmEPAgentPath(String apmEPAgentPath) {
		this.apmEPAgentPath = apmEPAgentPath;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public int getApmPort() {
		return apmPort;
	}

	public void setApmPort(int apmPort) {
		this.apmPort = apmPort;
	}

    public int getReportInMinutes() {
        return reportInMinutes;
    }

    public void setReportInMinutes(int reportInMinutes) {
        this.reportInMinutes = reportInMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
