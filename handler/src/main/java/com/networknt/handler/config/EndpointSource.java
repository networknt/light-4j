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