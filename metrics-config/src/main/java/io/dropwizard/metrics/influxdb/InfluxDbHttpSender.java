/*
 * Copyright 2010-2013 Coda Hale and Yammer, Inc., 2014-2017 Dropwizard Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dropwizard.metrics.influxdb;

import com.networknt.exception.ClientException;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.HttpMethod;
import com.networknt.metrics.TimeSeriesDbSender;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of InfluxDbSender that writes to InfluxDb via http.
 */
public class InfluxDbHttpSender implements TimeSeriesDbSender {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDbReporter.class);
    private final HttpClientRequest httpClientRequest = new HttpClientRequest();

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
        if(logger.isInfoEnabled()) logger.info("InfluxDbHttpSender is created with path = " + path + " and host = " + url);
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

        HttpRequest.Builder builder = httpClientRequest.initBuilder(this.url.toString() + this.path, HttpMethod.POST, Optional.of(body));
        builder.setHeader("Content-Type", "text/plain");
        HttpResponse<String> response = (HttpResponse<String>) httpClientRequest.send(builder, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if(statusCode >= 200 && statusCode < 300) {
            return statusCode;
        } else {
            logger.error("Server returned HTTP response code: " + statusCode
                    + "for path: " + path + " and host: " + url
                    + " with content :'"
                    + response.body() + "'");
            throw new ClientException("Server returned HTTP response code: " + statusCode
                    + "for path: " + path + " and host: " + url
                    + " with content :'"
                    + response.body() + "'");
        }
    }

    @Override
    public void setTags(final Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }
}
