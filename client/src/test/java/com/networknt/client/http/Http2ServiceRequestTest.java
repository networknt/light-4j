package com.networknt.client.http;


import com.networknt.status.HttpStatus;
import io.undertow.util.Methods;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.*;


public class Http2ServiceRequestTest {

    private Http2ServiceRequest  http2ServiceRequest;
    List<HttpStatus> statusCodesValid;

    @Before
    public void setUp() throws  Exception{
        http2ServiceRequest = new Http2ServiceRequest(new URI("http://localhost:7080"), Methods.GET);
        statusCodesValid = new ArrayList<>();
        statusCodesValid.add(HttpStatus.OK);
        statusCodesValid.add(HttpStatus.CREATED);
    }


    @Test
    public void testSuccessStatus() throws Exception{

        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(200));
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(201));
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(203));
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(204));
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(211));
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(303));
    }

    @Test
    public void testErrorStatus() throws Exception{

        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(400));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(404));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(408));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(500));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(505));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(502));
    }

    @Test
    public void testStatusByDefine() throws Exception{
        http2ServiceRequest.setStatusCodesValid(statusCodesValid);
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(200));
        assertTrue(http2ServiceRequest.optionallyValidateClientResponseStatusCode(201));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(203));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(204));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(226));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(303));
        assertFalse(http2ServiceRequest.optionallyValidateClientResponseStatusCode(400));
    }

}
