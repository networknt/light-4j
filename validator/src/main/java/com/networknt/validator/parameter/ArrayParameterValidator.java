package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import com.networknt.validator.report.ValidationReport;
import com.networknt.validator.SchemaValidator;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A validator for array parameters.
 * <p>
 * This is a special-case validator as it needs to handle single and collection types for validation.
 */
public class ArrayParameterValidator extends BaseParameterValidator {

    public static final String ARRAY_PARAMETER_TYPE = "array";

    private final SchemaValidator schemaValidator;

    private enum CollectionFormat {
        CSV(","),
        SSV(" "),
        TSV("\t"),
        PIPES("\\|"),
        MULTI(null);

        final String separator;
        CollectionFormat(String separator) {
            this.separator = separator;
        }

        Collection<String> split(final String value) {
            if (separator == null) {
                return Collections.singleton(value);
            }
            return Arrays.asList(value.split(separator));
        }

        static CollectionFormat from(final SerializableParameter parameter) {
            requireNonNull(parameter, "A parameter is required");
            return valueOf(parameter.getCollectionFormat().toUpperCase());
        }
    }

    public ArrayParameterValidator(final SchemaValidator schemaValidator,
                                   final MessageResolver messages) {
        super(messages);
        this.schemaValidator = schemaValidator == null ? new SchemaValidator(messages) : schemaValidator;
    }

    @Override
    public String supportedParameterType() {
        return ARRAY_PARAMETER_TYPE;
    }

    @Override
    public ValidationReport validate(final String value, final Parameter p) {
        final MutableValidationReport report = new MutableValidationReport();

        if (!supports(p)) {
            return report;
        }

        final SerializableParameter parameter = (SerializableParameter)p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            return report.add(messages.get("validation.request.parameter.missing", parameter.getName()));
        }

        if (value == null || value.trim().isEmpty()) {
            return report;
        }

        doValidate(value, parameter, report);
        return report;
    }

    public ValidationReport validate(final Collection<String> values, final Parameter p) {
        final MutableValidationReport report = new MutableValidationReport();
        if (p == null) {
            return report;
        }

        final SerializableParameter parameter = (SerializableParameter)p;
        if (parameter.getRequired() && (values == null || values.isEmpty())) {
            return report.add(messages.get("validation.request.parameter.missing", parameter.getName()));
        }

        if (values == null) {
            return report;
        }

        if (!parameter.getCollectionFormat().equalsIgnoreCase(CollectionFormat.MULTI.name())) {
            return report.add(messages.get("validation.request.parameter.collection.invalidFormat",
                    p.getName(), parameter.getCollectionFormat(), "multi")
            );
        }

        doValidate(values, parameter, report);
        return report;
    }

    @Override
    protected void doValidate(final String value,
                              final SerializableParameter parameter,
                              final MutableValidationReport validationReport) {

        doValidate(CollectionFormat.from(parameter).split(value),
                parameter,
                validationReport);

    }

    private void doValidate(final Collection<String> values,
                            final SerializableParameter parameter,
                            final MutableValidationReport validationReport) {

        if (parameter.getMaxItems() != null && values.size() > parameter.getMaxItems()) {
            validationReport.add(messages.get("validation.request.parameter.collection.tooManyItems",
                    parameter.getName(), parameter.getMaxItems(), values.size())
            );
        }

        if (parameter.getMinItems() != null && values.size() < parameter.getMinItems()) {
            validationReport.add(messages.get("validation.request.parameter.collection.tooFewItems",
                    parameter.getName(), parameter.getMinItems(), values.size())
            );
        }

        if (Boolean.TRUE.equals(parameter.isUniqueItems()) &&
                values.stream().distinct().count() != values.size()) {
            validationReport.add(messages.get("validation.request.parameter.collection.duplicateItems",
                    parameter.getName())
            );
        }

        if (parameter.getEnum() != null && !parameter.getEnum().isEmpty()) {
            final Set<String> enumValues = new HashSet<>(parameter.getEnum());
            values.stream()
                    .filter(v -> !enumValues.contains(v))
                    .forEach(v -> {
                        validationReport.add(messages.get("validation.request.parameter.enum.invalid",
                                v, parameter.getName(), parameter.getEnum())
                        );
                    });
            return;
        }

        values.forEach(v ->
                validationReport.addAll(schemaValidator.validate(v, parameter.getItems())));
    }
}
