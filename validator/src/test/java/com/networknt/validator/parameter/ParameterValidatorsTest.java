package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import org.junit.Test;

import static com.networknt.validator.ValidatorTestUtil.assertFail;
import static com.networknt.validator.ValidatorTestUtil.assertPass;
import static com.networknt.validator.ValidatorTestUtil.floatParam;
import static com.networknt.validator.ValidatorTestUtil.intParam;
import static com.networknt.validator.ValidatorTestUtil.stringParam;

public class ParameterValidatorsTest {

    private final ParameterValidators parameterValidators = new ParameterValidators(null, new MessageResolver());

    @Test
    public void validate_withInvalidIntegerParam_shouldFail() {
        assertFail(parameterValidators.validate("1.0", intParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withValidIntegerParam_shouldPass() {
        assertPass(parameterValidators.validate("10", intParam()));
    }

    @Test
    public void validate_withInvalidNumberParam_shouldFail() {
        assertFail(parameterValidators.validate("1.0a", floatParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withValidNumberParam_shouldPass() {
        assertPass(parameterValidators.validate("1.0", floatParam()));
    }

    @Test
    public void validate_withInvalidStringParam_shouldFail() {
        assertFail(parameterValidators.validate("", stringParam()), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withValidStringParam_shouldPass() {
        assertPass(parameterValidators.validate("a", stringParam()));
    }

}
