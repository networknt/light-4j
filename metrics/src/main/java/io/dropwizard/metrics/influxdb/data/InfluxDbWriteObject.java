package io.dropwizard.metrics.influxdb.data;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class contains the request object to be sent to InfluxDb for writing. It contains a collection of points.
 */
public class InfluxDbWriteObject {

    private String precision;

    private Set<InfluxDbPoint> points;

    private Map<String, String> tags = Collections.emptyMap();

    public InfluxDbWriteObject(final TimeUnit timeUnit) {
        this.points = new HashSet<>();
        this.precision = toTimePrecision(timeUnit);
    }

    private static String toTimePrecision(TimeUnit t) {
        switch (t) {
            case HOURS:
                return "h";
            case MINUTES:
                return "m";
            case SECONDS:
                return "s";
            case MILLISECONDS:
                return "ms";
            case MICROSECONDS:
                return "u";
            case NANOSECONDS:
                return "n";
            default:
                throw new IllegalArgumentException(
                        "time precision should be HOURS OR MINUTES OR SECONDS or MILLISECONDS or MICROSECONDS OR NANOSECONDS");
        }
    }

    public String getPrecision() {
        return precision;
    }

    public void setPrecision(String precision) {
        this.precision = precision;
    }

    public Set<InfluxDbPoint> getPoints() {
        return points;
    }

    public void setPoints(Set<InfluxDbPoint> points) {
        this.points = points;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = Collections.unmodifiableMap(tags);
    }

    public String getBody() {
        StringJoiner joiner = new StringJoiner("\n");
        for (Object point : points) {
            joiner.add(point.toString());
        }
        return joiner.toString();
    }
}
