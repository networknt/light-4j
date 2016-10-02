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

import static com.networknt.validator.ValidatorTestUtil.floatParam;
import static com.networknt.validator.ValidatorTestUtil.intParam;
import static com.networknt.validator.ValidatorTestUtil.stringParam;

public class ParameterValidatorsTest {

    private final ParameterValidators parameterValidators = new ParameterValidators(null);

    @Test
    public void validate_withInvalidIntegerParam_shouldFail() {
        Status status = parameterValidators.validate("1.0", intParam());
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11010", status.getCode()); // request parameter invalid format
    }

    @Test
    public void validate_withValidIntegerParam_shouldPass() {
        Assert.assertNull(parameterValidators.validate("10", intParam()));
    }

    @Test
    public void validate_withInvalidNumberParam_shouldFail() {
        Status status = parameterValidators.validate("1.0a", floatParam());
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11010", status.getCode()); // request parameter invalid format
    }

    @Test
    public void validate_withValidNumberParam_shouldPass() {
        Assert.assertNull(parameterValidators.validate("1.0", floatParam()));
    }

    @Test
    public void validate_withInvalidStringParam_shouldFail() {
        Status status = parameterValidators.validate("", stringParam());
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }

    @Test
    public void validate_withValidStringParam_shouldPass() {
        Assert.assertNull(parameterValidators.validate("a", stringParam()));
    }

}
