package com.networknt.utility;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * Created by steve on 12/07/17.
 */
public class NioUtilsTest {

    @Test
    public void testTempDir() {
        String tempDir = NioUtils.getTempDir();
        System.out.println("tempDir = " + tempDir);
    }

    @Test
    public void testList() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("rest.zip");
        NioUtils.list(url.getPath());
    }

    @Test
    public void testUnzip() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("rest.zip");
        NioUtils.unzip(url.getPath(), NioUtils.getTempDir());
    }

}
