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

import com.networknt.client.model.ServiceDef;

import java.util.Map;

public interface RestClient {



    /**
     * Retrieve a representation by doing a GET on the specified URL.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param url the URL example: https://localhost:8443 ;  https://"com.networknt.hello-1
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @return the converted object
     */
    <T> T get(String url, String path, Class<T> responseType) throws RestClientException;

    /**
     * Retrieve a representation by doing a GET on the specified URL.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @return the converted object
     */
    <T> T get(ServiceDef serviceDef, String path, Class<T> responseType) throws RestClientException;

    /**
     * Retrieve a representation by doing a GET on the specified URL.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @param headerMap the map containing header setting map
     * @return the converted object
     */
    <T> T get(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap) throws RestClientException;

    /**
     * Retrieve a representation by doing a GET on the specified URL.
     * The response return with Json format string.
     * @param url the URL example: https://localhost:8443 ;  https://"com.networknt.hello-1
     * @param path the path of the service call, for example: /v1/hello
     * @return the JSON format object
     */
    String get(String url, String path) throws RestClientException;

    /**
     * Retrieve a representation by doing a GET on the specified URL.
     * The response return with Json format string.
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @return the JSON format object
     */
    String get(ServiceDef serviceDef, String path) throws RestClientException;
    /**
     * Retrieve a representation by doing a GET on the URI template.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param url the URL
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @param headerMap the map containing header setting map
     * @return the converted object
     */
    <T> T get(String url,  String path, Class<T> responseType, Map<String, ?> headerMap) throws RestClientException;

    /**
     * Retrieve a representation by doing a POST on the specified URL.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param url the URL example: https://localhost:8443 ;  https://"com.networknt.hello-1
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @param requestBody REQUEST BODY
     * @return the converted object
     */
    <T> T post(String url, String path, Class<T> responseType, String requestBody) throws RestClientException;

    /**
     * Retrieve a representation by doing a POST on the specified URL.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @param requestBody REQUEST BODY
     * @return the converted object
     */
    <T> T post(ServiceDef serviceDef, String path, Class<T> responseType, String requestBody) throws RestClientException;
    /**
     * Retrieve a representation by doing a POST on the specified URL.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @param headerMap the map containing header setting map
     * @param requestBody REQUEST BODY
     * @return the converted object
     */
    <T> T post(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap, String requestBody) throws RestClientException;
    /**
     * Retrieve a representation by doing a POST on the specified URL.
     * The response return with Json format string.
     * @param url the URL example: https://localhost:8443 ;  https://"com.networknt.hello-1
     * @param path the path of the service call, for example: /v1/hello
     * @param requestBody REQUEST BODY
     * @return the JSON format object
     */
    String post(String url, String path,  String requestBody) throws RestClientException;

    /**
     * Retrieve a representation by doing a POST on the specified URL.
     * The response return with Json format string.
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param requestBody REQUEST BODY
     * @return the JSON format object
     */
    String post(ServiceDef serviceDef, String path,  String requestBody) throws RestClientException;
    /**
     * Retrieve a representation by doing a POST on the URI template.
     * The response (if any) is converted and returned.
     * Use responseType as ClientResponse.class to get back the full response object
     * @param url the URL
     * @param path the path of the service call, for example: /v1/hello
     * @param responseType the type of the return value
     * @param headerMap the map containing header setting map
     * @param requestBody REQUEST BODY
     * @return the converted object
     */
    <T> T post(String url,  String path, Class<T> responseType, Map<String, ?> headerMap,  String requestBody) throws RestClientException;

    /**
     * Retrieve a representation by doing a PUT on the specified URL.
     * The response return with Json format string.
     * @param url the URL example: https://localhost:8443 ;  https://"com.networknt.hello-1
     * @param path the path of the service call, for example: /v1/hello
     * @param requestBody REQUEST BODY
     * @return the JSON format string
     */
    String put(String url, String path,  String requestBody) throws RestClientException;

    /**
     * Retrieve a representation by doing a PUT on the specified URL.
     * The response return with Json format string.
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param requestBody REQUEST BODY
     * @return the JSON format string
     */
    String put(ServiceDef serviceDef, String path,  String requestBody) throws RestClientException;

    /**
     * Retrieve a representation by doing a PUT on the specified URL.
     * The response return with Json format string.
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @param headerMap the map containing header setting map
     * @param requestBody REQUEST BODY
     * @return the JSON format string
     */
    String put(ServiceDef serviceDef, String path,  Map<String, ?> headerMap, String requestBody) throws RestClientException;
    /**
     * Retrieve a representation by doing a PUT on the URI template.
     * The response (if any) is converted and returned.
     * @param url the URL
     * @param path the path of the service call, for example: /v1/hello
     * @param headerMap the map containing header setting map
     * @param requestBody REQUEST BODY
     * @return the converted object
     */
   String put(String url,  String path,  Map<String, ?> headerMap,  String requestBody) throws RestClientException;

    /**
     * Retrieve a representation by doing a DELETE on the specified URL.
     * The response return with Json format string.
     * @param url the URL example: https://localhost:8443 ;  https://"com.networknt.hello-1
     * @param path the path of the service call, for example: /v1/hello
     * @return the JSON format string
     */
    String delete(String url, String path) throws RestClientException;

    /**
     * Retrieve a representation by doing a DELETE on the specified URL.
     * The response return with Json format string.
     * @param serviceDef the URL service definition
     * @param path the path of the service call, for example: /v1/hello
     * @return the JSON format string
     */
    String delete(ServiceDef serviceDef, String path) throws RestClientException;
    /**
     * Retrieve a representation by doing a PUT on the URI template.
     * The response (if any) is converted and returned.
     * @param url the URL
     * @param path the path of the service call, for example: /v1/hello
     * @param headerMap the map containing header setting map
     * @return the converted object
     */
    String delete(String url,  String path,  Map<String, ?> headerMap,  String requestBody) throws RestClientException;
}
