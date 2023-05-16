package com.networknt.metrics;

import org.junit.Test;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IssuerRegexTest {
    Matcher m1 = Pattern.compile("/([^/]+)$").matcher("https://sunlifeapi.oktapreview.com/oauth2/aus9xt6dd1cSYyRPH1d6");

    @Test
    public void testOktaIssMatcher1() {
        if(m1.find()) {
            System.out.println(m1.group(1));
        }
    }

}
