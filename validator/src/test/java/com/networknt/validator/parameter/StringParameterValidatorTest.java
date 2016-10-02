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
import org.junit.Assert;
import org.junit.Test;

import static com.networknt.validator.ValidatorTestUtil.stringParam;

public class StringParameterValidatorTest {

    private StringParameterValidator classUnderTest = new StringParameterValidator();

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate(null, stringParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate("", stringParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        Status status = classUnderTest.validate(null, stringParam(true));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        Status status = classUnderTest.validate("", stringParam(true));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }
}
