package com.networknt.metrics;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsConfigTest {
    @Test
    public void testMetricsIss() {
        MetricsConfig config = MetricsConfig.load();
        System.out.println(config.getIssuerRegex());
        if(config.getIssuerRegex() != null) {
            Pattern pattern = Pattern.compile(config.getIssuerRegex());
            Matcher matcher = pattern.matcher("https://sunlifeapi.oktapreview.com/oauth2/aus9xt6dd1cSYyRPH1d6");
            if(matcher.find()) {
                System.out.println(matcher.group(1));
            }
        }
    }
}
