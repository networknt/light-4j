package com.networknt.handler.config;

import com.networknt.config.ConfigException;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * URL rewrite rule
 */
public class UrlRewriteRule {
    private static final Logger LOG = LoggerFactory.getLogger(UrlRewriteRule.class);

    Pattern pattern;
    String replace;

    /**
     * Constructor
     * @param pattern regex pattern
     * @param replace replace string
     */
    public UrlRewriteRule(Pattern pattern, String replace) {
        this.pattern = pattern;
        this.replace = replace;
    }

    /**
     * Get the regex pattern
     * @return regex pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Set the regex pattern
     * @param pattern regex pattern
     */
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Get the replace string
     * @return replace string
     */
    public String getReplace() {
        return replace;
    }

    /**
     * Set the replace string
     * @param replace replace string
     */
    public void setReplace(String replace) {
        this.replace = replace;
    }

    /**
     * Convert string to UrlRewriteRule
     * @param s string
     * @return UrlRewriteRule
     */
    public static UrlRewriteRule convertToUrlRewriteRule(String s) {
        // make sure that the string has two parts and the first part can be compiled to a pattern.
        var parts = StringUtils.split(s, ' ');

        if (parts.length != 2) {
            var error = "The URL rewrite rule " + s + " must have two parts";

            if (LOG.isErrorEnabled())
                LOG.error(error);

            throw new ConfigException(error);
        }

        return new UrlRewriteRule(Pattern.compile(parts[0]), parts[1]);
    }

}
