package com.networknt.validator;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by steve on 17/09/16.
 */
public class ValidatorConfig {
    boolean enableValidator;
    boolean enableResponseValidator;

    @JsonIgnore
    String description;

    public ValidatorConfig() {
    }

    public boolean isEnableValidator() {
        return enableValidator;
    }

    public void setEnableValidator(boolean enableValidator) {
        this.enableValidator = enableValidator;
    }

    public boolean isEnableResponseValidator() {
        return enableResponseValidator;
    }

    public void setEnableResponseValidator(boolean enableResponseValidator) {
        this.enableResponseValidator = enableResponseValidator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
