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

import com.networknt.validator.report.MessageResolver;
import org.junit.Test;

import static com.networknt.validator.ValidatorTestUtil.assertFail;
import static com.networknt.validator.ValidatorTestUtil.assertPass;
import static com.networknt.validator.ValidatorTestUtil.stringParam;

public class StringParameterValidatorTest {

    private StringParameterValidator classUnderTest = new StringParameterValidator(new MessageResolver());

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate(null, stringParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", stringParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate(null, stringParam(true)), "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", stringParam(true)), "validation.request.parameter.missing");
    }

}
