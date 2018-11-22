package com.networknt;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HttpClientUtils {
    private HttpClientUtils() {

    }

    public static String readResponse(final HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if(entity == null) {
            return "";
        }
        return readResponse(entity.getContent());
    }

    public static String readResponse(InputStream stream) throws IOException {

        byte[] data = new byte[100];
        int read;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = stream.read(data)) != -1) {
            out.write(data, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public static byte[] readRawResponse(final HttpResponse response) throws IOException {
        return readRawResponse(response.getEntity().getContent());
    }

    public static byte[] readRawResponse(InputStream stream) throws IOException {
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] data = new byte[100];
        int read;
        while ((read = stream.read(data)) != -1) {
            b.write(data, 0, read);
        }
        return b.toByteArray();
    }
}
