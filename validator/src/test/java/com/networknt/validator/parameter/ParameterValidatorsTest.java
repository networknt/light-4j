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
