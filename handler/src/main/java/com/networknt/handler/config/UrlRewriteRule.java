package com.networknt.handler.config;

import com.networknt.config.ConfigException;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class UrlRewriteRule {
    public static final Logger logger = LoggerFactory.getLogger(UrlRewriteRule.class);

    Pattern pattern;
    String replace;

    public UrlRewriteRule(Pattern pattern, String replace) {
        this.pattern = pattern;
        this.replace = replace;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getReplace() {
        return replace;
    }

    public void setReplace(String replace) {
        this.replace = replace;
    }

    public static UrlRewriteRule convertToUrlRewriteRule(String s) {
        // make sure that the string has two parts and the first part can be compiled to a pattern.
        String[] parts = StringUtils.split(s, ' ');
        if(parts.length != 2) {
            String error = "The URL rewrite rule " + s + " must have two parts";
            logger.error(error);
            throw new ConfigException(error);
        }
        return new UrlRewriteRule(Pattern.compile(parts[0]), parts[1]);
    }

}
