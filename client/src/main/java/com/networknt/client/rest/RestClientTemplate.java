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

package com.networknt.client.rest;

import com.networknt.client.http.Http2ServiceRequest;
import com.networknt.client.model.ServiceDef;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.Map;

public class RestClientTemplate implements RestClient {

    private static Logger logger = LoggerFactory.getLogger(RestClientTemplate.class);

    private OptionMap restOptions;

    /**
     * Instantiate a new LightRestClient with default RestClientOptions
     */
    public RestClientTemplate() {
        this.restOptions = OptionMap.EMPTY;
    }

    /**
     * Instantiate a new LightRestClient with configurable RestClientOptions
     * @param restOptions org.xnio.OptionMap of RestClientOptions
     */
    public RestClientTemplate(OptionMap restOptions) {
        this.restOptions = restOptions != null ? restOptions : OptionMap.EMPTY;
    }

    @Override
    public String get(String url, String path) throws RestClientException {
        return get(url, path, String.class);
    }

    @Override
    public <T> T get(String url, String path, Class<T> responseType) throws RestClientException {
        return get(url, path, responseType, null);
    }

    @Override
    public <T> T get(ServiceDef serviceDef, String path, Class<T> responseType) throws RestClientException {
        return execute(serviceDef, path, responseType, null, Methods.GET, null);
    }

    @Override
    public <T> T get(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap) throws RestClientException {
        return execute(serviceDef, path, responseType, headerMap, Methods.GET, null);
    }

    @Override
    public String get(ServiceDef serviceDef, String path) throws RestClientException {
        return get(serviceDef, path, String.class);
    }

    @Override
    public <T> T get(String url, String path, Class<T> responseType, Map<String, ?> headerMap) throws RestClientException {
        return execute(url, path, responseType, headerMap, Methods.GET, null);
    }

    @Override
    public <T> T post(String url, String path, Class<T> responseType, String requestBody) throws RestClientException {
        return post(url, path, responseType, null, requestBody);
    }

    @Override
    public String post(String url, String path, String requestBody) throws RestClientException {
        return post(url, path, String.class, requestBody);
    }

    @Override
    public <T> T post(String url, String path, Class<T> responseType, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(url, path, responseType, headerMap, Methods.POST, requestBody);
    }

    @Override
    public String post(ServiceDef serviceDef, String path, String requestBody) throws RestClientException {
        return post(serviceDef, path, String.class, requestBody);
    }

    @Override
    public <T> T post(ServiceDef serviceDef, String path, Class<T> responseType, String requestBody) throws RestClientException {
        return post(serviceDef, path, responseType, null, requestBody);
    }

    @Override
    public <T> T post(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(serviceDef, path, responseType, headerMap, Methods.POST, requestBody);
    }

    @Override
    public String put(String url, String path, String requestBody) throws RestClientException {
        return put(url, path, null, requestBody);
    }

    @Override
    public String put(String url, String path, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(url, path, String.class, headerMap, Methods.PUT, requestBody);
    }

    @Override
    public String put(ServiceDef serviceDef, String path, String requestBody) throws RestClientException {
        return execute(serviceDef, path, String.class, null, Methods.PUT, requestBody);
    }

    @Override
    public String put(ServiceDef serviceDef, String path, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(serviceDef, path, String.class, headerMap, Methods.PUT, requestBody);
    }

    @Override
    public String delete(String url, String path) throws RestClientException {
        return delete(url, path, null, null);
    }

    @Override
    public String delete(String url, String path, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(url, path, String.class, headerMap, Methods.DELETE, requestBody);
    }

    @Override
    public String delete(ServiceDef serviceDef, String path) throws RestClientException {
        return execute(serviceDef, path, String.class, null, Methods.DELETE, null);
    }

    protected <T> T execute(String url, String path, Class<T> responseType, Map<String, ?> headerMap, HttpString method, String requestBody) throws RestClientException {
        try {
            Http2ServiceRequest http2ServiceRequest = new Http2ServiceRequest(new URI(url), path, method);
            http2ServiceRequest.setRequestHeaders(headerMap);
            if (requestBody!=null) http2ServiceRequest.setRequestBody(requestBody);
            return http2ServiceRequest.callForTypedObject(responseType).get();
        } catch (Exception e) {
            String errorStr = "execute the restful API call error:";
            logger.error(errorStr + e);
            throw  new RestClientException(errorStr, e);
        }
    }

    protected <T> T execute(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap, HttpString method, String requestBody) throws RestClientException {
        try {
            Http2ServiceRequest http2ServiceRequest = new Http2ServiceRequest(serviceDef, path, method);
            http2ServiceRequest.setRequestHeaders(headerMap);
            if (requestBody!=null) http2ServiceRequest.setRequestBody(requestBody);
            return http2ServiceRequest.callForTypedObject(responseType).get();
        } catch (Exception e) {
            String errorStr = "execute the restful API call error:";
            logger.error(errorStr + e);
            throw  new RestClientException(errorStr, e);
        }

    }

}
