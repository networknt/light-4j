package com.networknt.client.http;

import com.networknt.common.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class HttpRequestValueTest {

    private String  fileName;
    private byte[]  fileBody;

    @BeforeEach
    public void setUp() {
        fileName = "{\"filename\": \"sample.pdf\"}";
        String content = "sample pdf file input string";
        fileBody = content.getBytes();
    }


    @Test
    public void testBuildRequest(){

        HttpRequestValue requestValue = HttpRequestValue.builder(ContentType.MULTIPART_MIXED).with("file-info", ContentType.APPLICATION_JSON, fileName)
        .with("file-body", ContentType.APPLICATION_PDF, fileBody).build();
        assertTrue(requestValue.hasBody("file-body"));
        assertTrue(requestValue.hasBody("file-info"));
        assertNotNull(requestValue.getBody("file-info"));
    }

}
