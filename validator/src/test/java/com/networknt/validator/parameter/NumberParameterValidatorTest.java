package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import org.junit.Test;

import static com.networknt.validator.ValidatorTestUtil.assertFail;
import static com.networknt.validator.ValidatorTestUtil.assertPass;
import static com.networknt.validator.ValidatorTestUtil.floatParam;

public class NumberParameterValidatorTest {

    private NumberParameterValidator classUnderTest = new NumberParameterValidator(new MessageResolver());

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(null, floatParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", floatParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(null, floatParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", floatParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNonNumericValue_shouldFail() {
        assertFail(classUnderTest.validate("not-a-Number", floatParam()), "validation.request.parameter.invalidFormat");
    }

    @Test
    public void validate_withPositiveNumericValue_shouldPass() {
        assertPass(classUnderTest.validate("123.456", floatParam()));
    }

    @Test
    public void validate_withNegativeNumericValue_shouldPass() {
        assertPass(classUnderTest.validate("-123.456", floatParam()));
    }

    @Test
    public void validate_withValueGreaterThanMax_shouldFail_ifMaxSpecified() {
        assertFail(classUnderTest.validate("1.1", floatParam(null, 1.0)),
                "validation.request.parameter.number.aboveMax");
    }

    @Test
    public void validate_withValueLessThanMin_shouldFail_ifMinSpecified() {
        assertFail(classUnderTest.validate("0.9", floatParam(1.0, null)),
                "validation.request.parameter.number.belowMin");
    }

    @Test
    public void validate_withValueInRange_shouldPass() {
        assertPass(classUnderTest.validate("1.1", floatParam(1.0, 1.2)));
    }
}
