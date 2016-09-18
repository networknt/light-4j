package com.networknt.validator.parameter;

import com.networknt.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;

public interface ParameterValidator {

    String supportedParameterType();

    boolean supports(Parameter p);

    ValidationReport validate(String value, Parameter p);
}
