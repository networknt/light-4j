package com.networknt.dump;

public class DumpConstants {
    public static final String DUMP_METHOD_PREFIX = "dump";
    public static final String HEADERS = "headers";
    public static final String COOKIES = "cookies";
    public static final String QUERY_PARAMETERS = "queryParameters";
    public static final String BODY = "body";
    public static final String STATUS_CODE = "statusCode";

    public static final String[] REQUEST_OPTIONS = {HEADERS, COOKIES, QUERY_PARAMETERS, BODY};
    public static final String[] RESPONSE_OPTIONS = {HEADERS, COOKIES, BODY, STATUS_CODE};
}
