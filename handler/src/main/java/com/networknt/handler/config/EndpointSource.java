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

package com.networknt.handler.config;

import java.util.Objects;

/**
 * Interface for generating a sequence of path/method pairs for injecting into handler.yml
 */
public interface EndpointSource {

    final class Endpoint {

        private String path;
        private String method;

        public Endpoint(String path, String method) {
            this.path = path;
            this.method = method;
        }

        public String getPath() { return path; }

        public String getMethod() { return method; }

        @Override
        public String toString() {
            return path + "@" + method;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            Endpoint endpoint = (Endpoint)o;
            return Objects.equals(path, endpoint.path) &&
                Objects.equals(method, endpoint.method);
        }

        @Override
        public int hashCode() { return Objects.hash(path, method); }
    }

    Iterable<Endpoint> listEndpoints();

}