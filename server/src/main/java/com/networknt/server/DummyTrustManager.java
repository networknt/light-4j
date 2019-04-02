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

package com.networknt.server;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Unlike static site, microservices are deployed on cloud with docker containers. It is very
 * hard to have a static ip or domain associates with each service instance, especially when
 * docker orchestration tool like Kubernetes is in the picture. The service can be down on one
 * node but started on another node within seconds. TLS for microservices is for data encryption
 * majority of time.
 *
 * This DummyTrustManager is use by the server to trust all certs from when communicate with
 * other servers.
 *
 * @author Steve Hu
 */
public class DummyTrustManager implements X509TrustManager {

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {
    }
}
