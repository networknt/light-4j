/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package com.networknt.monad;

import com.networknt.status.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.networknt.monad.Success.SUCCESS;

public class ResultTest {
    @Test
    public void testResult() {
        Status status = new Status(400, "ERR00000", "DEMO_STATUS", "This is an error", "ERROR");
        Assertions.assertTrue(SUCCESS.isSuccess());
        Assertions.assertTrue(!Failure.of(status).isSuccess());
        Assertions.assertTrue(!SUCCESS.isFailure());
        Assertions.assertTrue(Failure.of(status).isFailure());

        Result result = Failure.of(status);
        Assertions.assertTrue(result.getError().equals(status));

        String stringResult = "String result";
        result = Success.of(stringResult);
        Assertions.assertEquals(stringResult, result.getResult());
   }
}
