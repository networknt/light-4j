package com.networknt.validator.parameter;

import com.networknt.validator.report.MessageResolver;
import io.swagger.models.properties.IntegerProperty;
import org.junit.Test;

import static com.networknt.validator.ValidatorTestUtil.arrayParam;
import static com.networknt.validator.ValidatorTestUtil.assertFail;
import static com.networknt.validator.ValidatorTestUtil.assertPass;
import static com.networknt.validator.ValidatorTestUtil.enumeratedArrayParam;
import static com.networknt.validator.ValidatorTestUtil.intArrayParam;
import static com.networknt.validator.ValidatorTestUtil.stringArrayParam;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayParameterValidatorTest {

    private ArrayParameterValidator classUnderTest = new ArrayParameterValidator(null, new MessageResolver());

    @Test
    public void validate_withValidCsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3", intArrayParam(true, "csv")));
    }

    @Test
    public void validate_withValidPipesFormat_shouldPass() {
        assertPass(classUnderTest.validate("1|2|3", intArrayParam(true, "pipes")));
    }

    @Test
    public void validate_withValidTsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1\t2\t3", intArrayParam(true, "tsv")));
    }

    @Test
    public void validate_withValidSsvFormat_shouldPass() {
        assertPass(classUnderTest.validate("1 2 3", intArrayParam(true, "ssv")));
    }

    @Test
    public void validate_withTrailingSeparator_shouldPass() {
        assertPass(classUnderTest.validate("1,2,3,", intArrayParam(true, "csv")));
    }

    @Test
    public void validate_withSingleValue_shouldPass() {
        assertPass(classUnderTest.validate("bob", stringArrayParam(true, "csv")));
    }

    @Test
    public void validate_withInvalidParameter_shouldFail() {
        assertFail(classUnderTest.validate("1,2.1,3", intArrayParam(true, "csv")),
                "validation.schema.type");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", intArrayParam(true, "csv")),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate((String) null, intArrayParam(true, "csv")),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", intArrayParam(false, "csv")));
    }

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate((String) null, intArrayParam(false, "csv")));
    }

    @Test
    public void validate_withCollection_shouldFail_whenNotMultiFormat() {
        assertFail(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, "csv")),
                "validation.request.parameter.collection.invalidFormat");
    }

    @Test
    public void validate_withCollection_shouldPass_whenMultiFormat() {
        assertPass(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, "multi")));
    }

    @Test
    public void validate_withInvalidCollectionParameter_shouldFail() {
        assertFail(classUnderTest.validate(asList("1", "2.1", "3"), intArrayParam(true, "multi")),
                "validation.schema.type");
    }

    @Test
    public void validate_withEmptyCollection_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(emptyList(), intArrayParam(true, "multi")),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyCollection_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(emptyList(), intArrayParam(false, "multi")));
    }

    @Test
    public void validate_withTooFewValues_shouldFail_whenMinItemsSpecified() {
        assertFail(classUnderTest.validate("1,2", arrayParam(true, "csv", 3, 5, null, new IntegerProperty())),
                "validation.request.parameter.collection.tooFewItems");
    }

    @Test
    public void validate_withTooManyValues_shouldFail_whenMaxItemsSpecified() {
        assertFail(classUnderTest.validate("1,2,3,4,5,6", arrayParam(true, "csv", 3, 5, null, new IntegerProperty())),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void validate_withNonUniqueValues_shouldFail_whenUniqueSpecified() {
        assertFail(classUnderTest.validate("1,2,1", arrayParam(true, "csv", null, null, true, new IntegerProperty())),
                "validation.request.parameter.collection.duplicateItems");
    }

    @Test
    public void validate_withNonUniqueValues_shouldPass_whenUniqueNotSpecified() {
        assertPass(classUnderTest.validate("1,2,1", arrayParam(true, "csv", null, null, false, new IntegerProperty())));
    }

    @Test
    public void validate_withEnumValues_whouldPass_whenAllValuesMatchEnum() {
        assertPass(classUnderTest.validate("1,2,1", enumeratedArrayParam(true, "csv", "1", "2", "3")));
    }

    @Test
    public void validate_withEnumValues_whouldFail_whenValueDoesntMatchEnum() {
        assertFail(classUnderTest.validate("1,2,1,4", enumeratedArrayParam(true, "csv", "1", "2", "bob")),
                "validation.request.parameter.enum.invalid");
    }
}
