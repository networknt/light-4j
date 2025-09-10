package io.dropwizard.metrics.broadcom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.HttpMethod;
import com.networknt.metrics.TimeSeriesDbSender;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class APMEPAgentSender implements TimeSeriesDbSender {
    private static final Logger logger = LoggerFactory.getLogger(APMEPAgentSender.class);
    private final String path;
    private final String serviceId;
    private final String productName;
    private final HttpClientRequest httpClientRequest = new HttpClientRequest();
    private final URL url;
    private final InfluxDbWriteObject influxDbWriteObject;

    public APMEPAgentSender(final String protocol, final String hostname, final int port, final String epAgentPath, final String serviceId, final String productName) throws MalformedURLException {
        this(protocol, hostname, port, epAgentPath, serviceId, productName, TimeUnit.MILLISECONDS);
    }

    public APMEPAgentSender(final String protocol, final String hostname, final int port, final String epAgentPath, final String serviceId, final String productName, final TimeUnit timePrecision) throws MalformedURLException {
        this.url = new URL(protocol, hostname, port, "");
        this.path = epAgentPath;
        this.serviceId = serviceId;
        this.productName = productName;
        if(logger.isInfoEnabled()) logger.info("APMEPAgentSender is created with path = {}  and host = {}", path, url);
        this.influxDbWriteObject = new InfluxDbWriteObject(timePrecision);
    }

    @Override
    public void flush() {
        influxDbWriteObject.setPoints(new HashSet<>());
    }

    @Override
    public boolean hasSeriesData() {
        return influxDbWriteObject.getPoints() != null && !influxDbWriteObject.getPoints().isEmpty();
    }

    @Override
    public void appendPoints(final InfluxDbPoint point) {
        if (point != null) {
            influxDbWriteObject.getPoints().add(point);
        }
    }

    @Override
    public int writeData() throws Exception {

        final String body = convertInfluxDBWriteObjectToJSON(influxDbWriteObject);
        if(logger.isTraceEnabled()) logger.trace("APMEPAgentSender is sending data to host = {} with body = {}", url, body);

        HttpRequest.Builder builder = httpClientRequest.initBuilder(this.url.toString() + this.path, HttpMethod.POST, Optional.of(body));
        builder.setHeader("Content-Type", "application/json");
        HttpResponse<String> response = (HttpResponse<String>) httpClientRequest.send(builder, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if(statusCode >= 200 && statusCode < 300) {
            return statusCode;
        } else {
            logger.error("Server returned HTTP response code: {} for path: {} and host: {} with content :'{}'",
            statusCode, path, url, response.body());
            throw new ClientException("Server returned HTTP response code: " + statusCode
                    + "for path: " + path + " and host: " + url
                    + " with content :'"
                    + response.body() + "'");
        }
    }

    private String convertInfluxDBWriteObjectToJSON(InfluxDbWriteObject influxDbWriteObject) throws ClientException {

    	EPAgentMetricRequest epAgentMetricRequest = new EPAgentMetricRequest();
    	List<EPAgentMetric> epAgentMetricList = new ArrayList<EPAgentMetric>();

        for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
        	EPAgentMetric epAgentMetric = new EPAgentMetric();
			epAgentMetric.setName(convertName(point));

			String pointValue = point.getValue();

            // Value contains a decimal, we need to round to the nearest whole number.
            if (pointValue.contains(".")) {
                double milliseconds = Double.parseDouble(point.getValue());
                int roundedMilliseconds = (int) Math.round(milliseconds);
                epAgentMetric.setValue(Integer.toString(roundedMilliseconds));

            // Value contains no decimal place, no need for conversion
            } else {
                epAgentMetric.setValue(pointValue);
            }

			epAgentMetric.setType("PerIntervalCounter");
			epAgentMetricList.add(epAgentMetric);
		}

    	epAgentMetricRequest.setMetrics(epAgentMetricList);

    	String json = "";
    	try {
			json = Config.getInstance().getMapper().writeValueAsString(epAgentMetricRequest);
		} catch (JsonProcessingException e) {
            throw new ClientException(e);
		}


    	return json;

    }

	private String convertName(InfluxDbPoint point) {

        StringJoiner metricNameJoiner = new StringJoiner("|");

        metricNameJoiner.add(productName);
        metricNameJoiner.add(serviceId);

        for (Entry<String, String> pair : point.getTags().entrySet()) {
            Object value = pair.getValue();
            if(value != null) {
                metricNameJoiner.add(pair.getValue());
            } else {
                metricNameJoiner.add("null");
            }
        }

		return metricNameJoiner.toString() + ":" + point.getMeasurement();
	}

    @Override
    public void setTags(final Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }
}
