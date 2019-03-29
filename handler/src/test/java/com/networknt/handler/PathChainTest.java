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

package com.networknt.handler;

import com.networknt.handler.config.PathChain;
import org.junit.Assert;
import org.junit.Test;

public class PathChainTest {

    @Test
    public void validate_Path() {
        PathChain chain = new PathChain();
        chain.setPath("/my/path");
        chain.setMethod("GET");
        chain.validate("unit test config");
    }

    @Test
    public void validate_Source() {
        PathChain chain = new PathChain();
        chain.setSource("a.source.Class");
        chain.validate("unit test config");
    }

    @Test
    public void validate_NeitherPathNorSource() {
        PathChain chain = new PathChain();
        chain.setPath("/my/path");
        chain.setMethod("MAGIC");
        try {
            chain.validate("unit test config");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            System.out.println(e.toString());
            String ex_message = "Bad paths element in unit test config [ Invalid HTTP method: MAGIC ]";
            Assert.assertEquals(ex_message, e.getMessage());
        }
    }

    @Test
    public void validate_BadMethod() {
        PathChain chain = new PathChain();
        chain.setMethod("GET");
        try {
            chain.validate("unit test config");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            System.out.println(e.toString());
            String ex_message = "Bad paths element in unit test config [ You must specify either path or source ]";
            Assert.assertEquals(ex_message, e.getMessage());
        }
    }

    @Test
    public void validate_SourceWithPathAndMethod() {
        PathChain chain = new PathChain();
        chain.setSource("some.source.Class");
        chain.setPath("/some/path");
        chain.setMethod("GET");
        try {
            chain.validate("some unit test");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            System.out.println(e.toString());
            String ex_message = "Bad paths element in some unit test [ " +
                "Conflicting source: some.source.Class and path: /some/path | " +
                "Conflicting source: some.source.Class and method: GET ]";
            Assert.assertEquals(ex_message, e.getMessage());
        }
    }

    @Test
    public void validate_SourceWithPath() {
        PathChain chain = new PathChain();
        chain.setSource("some.source.Class");
        chain.setPath("/some/path");
        try {
            chain.validate("some unit test");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            System.out.println(e.toString());
            String ex_message = "Bad paths element in some unit test [ Conflicting source: some.source.Class and path: /some/path ]";
            Assert.assertEquals(ex_message, e.getMessage());
        }
    }

    @Test
    public void validate_SourceWithMethod() {
        PathChain chain = new PathChain();
        chain.setSource("some.source.Class");
        chain.setMethod("GET");
        try {
            chain.validate("some unit test");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            System.out.println(e.toString());
            String ex_message = "Bad paths element in some unit test [ Conflicting source: some.source.Class and method: GET ]";
            Assert.assertEquals(ex_message, e.getMessage());
        }
    }

}
