package com.networknt.dump;

/**
 * enabled: true
 * enableMask: true
 * request:
 *   headers:
 *   - Authorization
 *   - Host
 *   - Transfer-Encoding
 *   cookies: true
 *   queryParameters: true
 *   body: true
 *
 * response:
 *   headers: true
 *   cookies: true
 *   body: true
 *   statusCode: true
 *   contentLength: true
 */
public class DumpConfig {
    static boolean isEnabled;
    static boolean enableMask;
    //request config options
    static boolean dumpRequest;
    static boolean dumpRequestHeaders;
    static String[] dumpRequestHeaderFilter;
    static boolean dumpRequestCookies;
    static boolean dumpQueryParameters;
    static boolean dumpBody;
    //response config options
    static boolean dumpResponse;
    static boolean dumpResponseHeaders;
    static String[] dumpResponseHeaderFilter;
    static boolean dumpResponseCookies;
    static boolean dumpResponseStatusCode;
    static boolean dumpResponseContentLength;
}
