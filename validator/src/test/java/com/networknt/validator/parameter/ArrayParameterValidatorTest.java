/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.validator.parameter;

import com.networknt.status.Status;
import io.swagger.models.properties.IntegerProperty;
import org.junit.Assert;
import org.junit.Test;

import static com.networknt.validator.ValidatorTestUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayParameterValidatorTest {

    private final ArrayParameterValidator classUnderTest = new ArrayParameterValidator(null);

    @Test
    public void validate_withValidCsvFormat_shouldPass() {
        Assert.assertNull(classUnderTest.validate("1,2,3", intArrayParam(true, "csv")));
    }

    @Test
    public void validate_withValidPipesFormat_shouldPass() {
        Assert.assertNull(classUnderTest.validate("1|2|3", intArrayParam(true, "pipes")));
    }

    @Test
    public void validate_withValidTsvFormat_shouldPass() {
        Assert.assertNull(classUnderTest.validate("1\t2\t3", intArrayParam(true, "tsv")));
    }

    @Test
    public void validate_withValidSsvFormat_shouldPass() {
        Assert.assertNull(classUnderTest.validate("1 2 3", intArrayParam(true, "ssv")));
    }

    @Test
    public void validate_withTrailingSeparator_shouldPass() {
        Assert.assertNull(classUnderTest.validate("1,2,3,", intArrayParam(true, "csv")));
    }

    @Test
    public void validate_withSingleValue_shouldPass() {
        Assert.assertNull(classUnderTest.validate("bob", stringArrayParam(true, "csv")));
    }

    @Test
    public void validate_withInvalidParameter_shouldFail() {
        Status status = classUnderTest.validate("1,2.1,3", intArrayParam(true, "csv"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11004", status.getCode()); // validator schema
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        Status status = classUnderTest.validate("", intArrayParam(true, "csv"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        Status status = classUnderTest.validate((String) null, intArrayParam(true, "csv"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate("", intArrayParam(false, "csv")));
    }

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate((String) null, intArrayParam(false, "csv")));
    }

    @Test
    public void validate_withCollection_shouldFail_whenNotMultiFormat() {
        Status status = classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, "csv"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11005", status.getCode()); // request parameter collection invalid format
    }

    @Test
    public void validate_withCollection_shouldPass_whenMultiFormat() {
        Assert.assertNull(classUnderTest.validate(asList("1", "2", "3"), intArrayParam(true, "multi")));
    }

    @Test
    public void validate_withInvalidCollectionParameter_shouldFail() {
        Status status = classUnderTest.validate(asList("1", "2.1", "3"), intArrayParam(true, "multi"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11004", status.getCode()); // validator schema
    }

    @Test
    public void validate_withEmptyCollection_shouldFail_whenRequired() {
        Status status = classUnderTest.validate(emptyList(), intArrayParam(true, "multi"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing

    }

    @Test
    public void validate_withEmptyCollection_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate(emptyList(), intArrayParam(false, "multi")));
    }

    @Test
    public void validate_withTooFewValues_shouldFail_whenMinItemsSpecified() {
        Status status = classUnderTest.validate("1,2", arrayParam(true, "csv", 3, 5, null, new IntegerProperty()));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11007", status.getCode()); // request parameter collection too few items
    }

    @Test
    public void validate_withTooManyValues_shouldFail_whenMaxItemsSpecified() {
        Status status = classUnderTest.validate("1,2,3,4,5,6", arrayParam(true, "csv", 3, 5, null, new IntegerProperty()));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11006", status.getCode()); // request parameter collection too many items
    }

    @Test
    public void validate_withNonUniqueValues_shouldFail_whenUniqueSpecified() {
        Status status = classUnderTest.validate("1,2,1", arrayParam(true, "csv", null, null, true, new IntegerProperty()));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11008", status.getCode()); // request parameter collection duplicate items
    }

    @Test
    public void validate_withNonUniqueValues_shouldPass_whenUniqueNotSpecified() {
        Assert.assertNull(classUnderTest.validate("1,2,1", arrayParam(true, "csv", null, null, false, new IntegerProperty())));
    }

    @Test
    public void validate_withEnumValues_shouldPass_whenAllValuesMatchEnum() {
        Assert.assertNull(classUnderTest.validate("1,2,1", enumeratedArrayParam(true, "csv", new IntegerProperty(), "1", "2", "3")));
    }

    @Test
    public void validate_withEnumValues_shouldFail_whenValueDoesntMatchEnum() {
        Status status = classUnderTest.validate("1,2,1,4", enumeratedArrayParam(true, "csv", new IntegerProperty(), "1", "2", "bob"));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11009", status.getCode()); // request parameter collection duplicate items
    }
}
