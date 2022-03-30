package com.networknt.handler.config;

import java.util.regex.Pattern;

public class UrlRewriteRule {
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
}
