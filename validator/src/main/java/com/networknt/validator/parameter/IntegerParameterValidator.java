package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

public class IntegerParameterValidator extends BaseParameterValidator {

    public IntegerParameterValidator(final MessageResolver messages) {
        super(messages);
    }

    @Override
    public String supportedParameterType() {
        return "integer";
    }

    @Override
    protected void doValidate(
            final String value,
            final SerializableParameter parameter,
            final MutableValidationReport report) {

        if (parameter.getFormat().equalsIgnoreCase("int32")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "int32", report);
                return;
            }
        } else if (parameter.getFormat().equalsIgnoreCase("int64")){
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                failFormatValidation(value, parameter, "int64", report);
                return;
            }
        }

        final Long d = Long.parseLong(value);
        if (parameter.getMinimum() != null && d < parameter.getMinimum()) {
            report.add(messages.get("validation.request.parameter.number.belowMin",
                    value, parameter.getName(), parameter.getMinimum())
            );
        }

        if (parameter.getMaximum() != null && d > parameter.getMaximum()) {
            report.add(messages.get("validation.request.parameter.number.aboveMax",
                    value, parameter.getName(), parameter.getMaximum())
            );
        }
    }

    private void failFormatValidation(
            final String value,
            final SerializableParameter parameter,
            final String format,
            final MutableValidationReport report) {

        report.add(messages.get("validation.request.parameter.invalidFormat",
                value, parameter.getName(), supportedParameterType(), format)
        );
    }
}
