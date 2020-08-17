package com.networknt.client.http;

import com.networknt.httpstring.ContentType;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;


import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class HttpResponseValueTest {

    private String  fileName;
    private byte[]  fileBody;

    @Before
    public void setUp() {
        fileName = "{\"filename\": \"sample.pdf\"}";
        String content = "sample pdf file input string";
        fileBody = content.getBytes();
    }




    @Test
    public void testBuildResponse() {

        HttpResponseValue responseValue = HttpResponseValue.builder().with("file-info", ContentType.APPLICATION_JSON, fileName)
        .with("file-body", ContentType.APPLICATION_PDF_VALUE, fileBody).build();
        assertTrue(responseValue.hasBody("file-body"));
        assertTrue(responseValue.hasBody("file-info"));
        assertNotNull(responseValue.getBody("file-info"));
    }

    @Test
    public void testResponseValue() throws Exception {
        HttpResponseValue responseValue = HttpResponseValue.builder().with("file-info", ContentType.APPLICATION_JSON, fileName)
                .with("file-body", ContentType.APPLICATION_PDF_VALUE, fileBody).build();

        //Serialize the response value by sending response as buffer bytes
        //createClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch, final ByteBuffer requestBody)
        ByteBuffer requestBody = ByteBuffer.wrap(serialize(responseValue));

        //Deserialize the response value back to HttpResponseValue
        HttpResponseValue result  = (HttpResponseValue)deserialize(requestBody.array());
        assertTrue(result.hasBody("file-body"));
        assertTrue(result.hasBody("file-info"));
    }

    public  void serialize(Serializable obj, OutputStream outputStream) throws Exception {
        if (outputStream == null) {
            throw new IllegalArgumentException("The OutputStream must not be null");
        } else {
            ObjectOutputStream out = null;

            try {
                out = new ObjectOutputStream(outputStream);
                out.writeObject(obj);
            } catch (IOException var11) {
                throw new Exception(var11);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException var10) {
                    ;
                }

            }

        }
    }

    public  byte[] serialize(Serializable obj) throws  Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        serialize(obj, baos);
        return baos.toByteArray();
    }

    public static Object deserialize(InputStream inputStream)  throws  Exception{
        if (inputStream == null) {
            throw new IllegalArgumentException("The InputStream must not be null");
        } else {
            ObjectInputStream in = null;

            Object var3;
            try {
                in = new ObjectInputStream(inputStream);
                var3 = in.readObject();
            } catch (ClassNotFoundException var13) {
                throw new Exception(var13);
            } catch (IOException var14) {
                throw new Exception(var14);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException var12) {
                    ;
                }

            }

            return var3;
        }
    }

    public static Object deserialize(byte[] objectData)  throws  Exception{
        if (objectData == null) {
            throw new IllegalArgumentException("The byte[] must not be null");
        } else {
            return deserialize((InputStream)(new ByteArrayInputStream(objectData)));
        }
    }

}
