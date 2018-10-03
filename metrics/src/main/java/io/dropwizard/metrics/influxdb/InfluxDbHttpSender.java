package io.dropwizard.metrics.influxdb;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.mask.Mask;
import com.networknt.status.Status;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import io.undertow.client.*;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of InfluxDbSender that writes to InfluxDb via http.
 */
public class InfluxDbHttpSender implements InfluxDbSender {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbReporter.class);
    private final Http2Client client = Http2Client.getInstance();

    private final URL url;
    private final String path;

    private final InfluxDbWriteObject influxDbWriteObject;

    /**
     * Creates a new http sender given connection details.
     *
     * @param protocol   the influxDb protocol
     * @param hostname   the influxDb hostname
     * @param port       the influxDb http port
     * @param database   the influxDb database to write to
     * @param username   the username used to connect to influxDb
     * @param password   the password used to connect to influxDb
     * @throws Exception exception while creating the influxDb sender(MalformedURLException)
     */
    public InfluxDbHttpSender(final String protocol, final String hostname, final int port, final String database, final String username, final String password) throws Exception {
        this(protocol, hostname, port, database, username, password, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new http sender given connection details.
     *
     * @param protocol   the influxDb protocol
     * @param hostname      the influxDb hostname
     * @param port          the influxDb http port
     * @param database      the influxDb database to write to
     * @param username      the influxDb username
     * @param password      the influxDb password
     * @param timePrecision the time precision of the metrics
     * @throws Exception exception while creating the influxDb sender(MalformedURLException)
     */
    public InfluxDbHttpSender(final String protocol, final String hostname, final int port, final String database, final String username, final String password,
                              final TimeUnit timePrecision) throws Exception {
        this.url = new URL(protocol, hostname, port, "");
        String queryDb = String.format("db=%s", URLEncoder.encode(database, "UTF-8"));
        String queryCredential = String.format("u=%s&p=%s", URLEncoder.encode(username, "UTF8"), URLEncoder.encode(password, "UTF8"));
        String queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        this.path = "/write?" + queryDb + "&" + queryCredential + "&" + queryPrecision;
        if(logger.isInfoEnabled()) logger.info("InfluxDbHttpSender is created with path = " + Mask.maskString(path, "uri") + " and host = " + url);
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
        final String body = influxDbWriteObject.getBody();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(this.url.toURI(), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
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
                    connection.sendRequest(request, client.createClientCallback(reference, latch, body));
                }
            });
            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        if(statusCode >= 200 && statusCode < 300) {
            return statusCode;
        } else {
            logger.error("Server returned HTTP response code: " + statusCode
                    + "for path: " + path + " and host: " + url
                    + " with content :'"
                    + reference.get().getAttachment(Http2Client.RESPONSE_BODY) + "'");
            throw new ClientException("Server returned HTTP response code: " + statusCode
                    + "for path: " + path + " and host: " + url
                    + " with content :'"
                    + reference.get().getAttachment(Http2Client.RESPONSE_BODY) + "'");
        }
    }

    @Override
    public void setTags(final Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }
}
