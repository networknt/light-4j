package io.dropwizard.metrics.broadcom;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.mask.Mask;
import com.networknt.metrics.TimeSeriesDbSender;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

public class APMEPAgentSender implements TimeSeriesDbSender {
    private static final Logger logger = LoggerFactory.getLogger(APMEPAgentSender.class);
    private final String path;
    private final String serviceId;
    private final String productName;

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
        if(logger.isInfoEnabled()) logger.info("APMEPAgentSender is created with path = {}  and host = {}", Mask.maskString(path, "uri"), url);
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
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(this.url.toURI(), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(path);
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    connection.sendRequest(request, client.createClientCallback(reference, latch, body));
                }
            });
            boolean requestStatus = latch.await(10, TimeUnit.SECONDS);
            if(!requestStatus) {
                logger.info("The APM metrics push request timed out");
            }
        } catch (Exception e) {
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        if(statusCode >= 200 && statusCode < 300) {
            return statusCode;
        } else {
            logger.error("Server returned HTTP response code: {} for path: {} and host: {} with content :'{}'",
            statusCode, path, url, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            throw new ClientException("Server returned HTTP response code: " + statusCode
                    + "for path: " + path + " and host: " + url
                    + " with content :'"
                    + reference.get().getAttachment(Http2Client.RESPONSE_BODY) + "'");
        }
    }

    private String convertInfluxDBWriteObjectToJSON(InfluxDbWriteObject influxDbWriteObject) throws ClientException {

    	EPAgentMetricRequest epAgentMetricRequest = new EPAgentMetricRequest();
    	List<EPAgentMetric> epAgentMetricList = new ArrayList<EPAgentMetric>();

        for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
        	EPAgentMetric epAgentMetric = new EPAgentMetric();
			epAgentMetric.setName(convertName(point));

			// Need to convert the value from milliseconds with a decimal to milliseconds as a whole number
			double milliseconds = Double.parseDouble(point.getValue());
			int roundedMilliseconds = (int) Math.round(milliseconds);

			epAgentMetric.setValue(Integer.toString(roundedMilliseconds));
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
