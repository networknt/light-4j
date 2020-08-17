package com.networknt.client.http;

import com.networknt.httpstring.ContentType;
import org.junit.Before;
import org.junit.Test;


import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class HttpRequestValueTest {

    private String  fileName;
    private byte[]  fileBody;

    @Before
    public void setUp() {
        fileName = "{\"filename\": \"sample.pdf\"}";
        String content = "sample pdf file input string";
        fileBody = content.getBytes();
    }


    @Test
    public void testBuildRequest(){

        HttpRequestValue requestValue = HttpRequestValue.builder(ContentType.MULTIPART_MIXED_VALUE).with("file-info", ContentType.APPLICATION_JSON, fileName)
        .with("file-body", ContentType.APPLICATION_PDF_VALUE, fileBody).build();
        assertTrue(requestValue.hasBody("file-body"));
        assertTrue(requestValue.hasBody("file-info"));
        assertNotNull(requestValue.getBody("file-info"));
    }

}
