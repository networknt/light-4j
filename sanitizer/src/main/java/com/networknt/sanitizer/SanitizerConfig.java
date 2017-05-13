package com.networknt.sanitizer;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Sanitizer configuration class
 *
 * @author Steve Hu
 */
public class SanitizerConfig {
    boolean enabled;
    boolean sanitizeBody;
    boolean sanitizeHeader;

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

}
