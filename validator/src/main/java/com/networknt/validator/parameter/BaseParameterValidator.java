package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import com.networknt.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;


import static java.util.Objects.requireNonNull;

abstract class BaseParameterValidator implements ParameterValidator {

    protected final MessageResolver messages;

    protected BaseParameterValidator(final MessageResolver messages) {
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    @Override
    public boolean supports(final Parameter p) {
        return p != null &&
                p instanceof SerializableParameter &&
                supportedParameterType().equalsIgnoreCase(((SerializableParameter)p).getType());
    }

    @Override
    public ValidationReport validate(final String value, final Parameter p) {
        final MutableValidationReport report = new MutableValidationReport();

        if (!supports(p)) {
            return report;
        }

        final SerializableParameter parameter = (SerializableParameter)p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            return report.add(messages.get("validation.request.parameter.missing", p.getName()));
        }

        if (value == null || value.trim().isEmpty()) {
            return report;
        }

        if (!matchesEnumIfDefined(value, parameter)) {
            return report.add(
                    messages.get("validation.request.parameter.enum.invalid",
                            value, parameter.getName(), parameter.getEnum())
            );
        }

        doValidate(value, parameter, report);
        return report;
    }

    private boolean matchesEnumIfDefined(final String value, final SerializableParameter parameter) {
        return parameter.getEnum() == null ||
                parameter.getEnum().isEmpty() ||
                parameter.getEnum().stream().anyMatch(value::equals);
    }

    /**
     * Perform type-specific validations
     *
     * @param value The value being validated
     * @param parameter The parameter the value is being validated against
     * @param validationReport The report to accumulate validation errors
     */
    protected abstract void doValidate(
            String value,
            SerializableParameter parameter,
            MutableValidationReport validationReport);
}
