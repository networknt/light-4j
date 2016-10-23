package com.networknt.sanitizer;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by steve on 22/10/16.
 */
public class SanitizerConfig {
    boolean enabled;
    boolean sanitizeBody;
    boolean sanitizeHeader;

    @JsonIgnore
    String description;

    public SanitizerConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSanitizeBody() {
        return sanitizeBody;
    }

    public void setSanitizeBody(boolean sanitizeBody) {
        this.sanitizeBody = sanitizeBody;
    }

    public boolean isSanitizeHeader() {
        return sanitizeHeader;
    }

    public void setSanitizeHeader(boolean sanitizeHeader) {
        this.sanitizeHeader = sanitizeHeader;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
