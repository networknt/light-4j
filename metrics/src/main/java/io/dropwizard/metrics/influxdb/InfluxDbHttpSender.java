package io.dropwizard.metrics.influxdb;

import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of InfluxDbSender that writes to InfluxDb via http.
 */
public class InfluxDbHttpSender implements InfluxDbSender {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbReporter.class);

    private final CloseableHttpClient closeableHttpClient;
    private final URL url;
    private final String username;
    private final String password;
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
        String endpoint = new URL(protocol, hostname, port, "/write").toString();
        String queryDb = String.format("db=%s", URLEncoder.encode(database, "UTF-8"));
        String queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        this.url = new URL(endpoint + "?" + queryDb + "&" + queryPrecision);

        this.closeableHttpClient = HttpClients.createDefault();
        this.username = username;
        this.password = password;
        if(logger.isInfoEnabled()) logger.info("InfluxDbHttpSender is created with url = " + url + " username = " + username);
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

    private RequestConfig getRequestConfig() {
        return RequestConfig
            .custom()
            .setConnectTimeout(1000)
            .setConnectionRequestTimeout(1000)
            .build();
    }

    private HttpClientContext getHttpClientContext() {
        HttpClientContext httpClientContext = null;
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty())
        {
            httpClientContext = HttpClientContext.create();
            AuthScope authScope = new AuthScope(url.getHost(), url.getPort());
            UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(username, password);
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(authScope, usernamePasswordCredentials);
            httpClientContext.setCredentialsProvider(credentialsProvider);
        }

        return httpClientContext;
    }

    @Override
    public int writeData() throws Exception {
        final String body = influxDbWriteObject.getBody();
        HttpPost httpPost = new HttpPost(this.url.toURI());
        httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        httpPost.setConfig(getRequestConfig());

        return closeableHttpClient.execute(httpPost, httpResponse -> {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(httpResponse.getEntity());
            if (statusCode >= 200 && statusCode < 300) {
                return statusCode;
            } else {
                logger.error("Server returned HTTP response code: " + statusCode
                        + "for URL: " + url
                        + " with content :'"
                        + httpResponse.getStatusLine().getReasonPhrase() + "'");
                throw new ClientProtocolException("Server returned HTTP response code: " + statusCode
                                                      + "for URL: " + url
                                                      + " with content :'"
                                                      + httpResponse.getStatusLine().getReasonPhrase() + "'" );
            }

        }, getHttpClientContext());
    }

    @Override
    public void setTags(final Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }
}
