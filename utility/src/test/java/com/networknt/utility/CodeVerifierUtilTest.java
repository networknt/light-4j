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

package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by steve on 22/06/17.
 */
public class CodeVerifierUtilTest {

    @Test
    public void testCodeVerifier() {
        String v = CodeVerifierUtil.generateRandomCodeVerifier();
        System.out.println("v = " + v);
        String c1 = CodeVerifierUtil.deriveCodeVerifierChallenge(v);
        System.out.println("c1 = " + c1);

        String c2 = CodeVerifierUtil.deriveCodeVerifierChallenge(v);
        System.out.println("c2 = " + c2);
        Assert.assertTrue(c1.equals(c2));
    }

    
}
