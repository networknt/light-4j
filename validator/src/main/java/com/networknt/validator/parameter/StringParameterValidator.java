package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

public class StringParameterValidator extends BaseParameterValidator {

    public StringParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    public String supportedParameterType() {
        return "string";
    }

    @Override
    protected void doValidate(
            final String value,
            final SerializableParameter parameter,
            final MutableValidationReport report) {
        // TODO: Check pattern etc.
    }
}
