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

package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        NioUtils.list(url.getPath().toString().replace("/C:/","C:\\"));
    }

    @Test
    public void testUnzip() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("rest.zip");
        NioUtils.unzip(url.getPath().toString().replace("/C:/","C:\\"), NioUtils.getTempDir());
    }

    @Test
    public void testToByteBuffer1() {
        String s = "こんにちは";
        ByteBuffer bb = NioUtils.toByteBuffer(s);
        String n = new String(bb.array(), StandardCharsets.UTF_8);
        Assert.assertEquals(s, n);
    }

    @Test
    public void testToByteBuffer2() {
        String s = "obufscate thdé alphebat and yolo!!";
        ByteBuffer bb = NioUtils.toByteBuffer(s);
        String n = new String(bb.array(), StandardCharsets.UTF_8);
        Assert.assertEquals(s, n);
    }

    @Test(expected = IOException.class)
    public void testUnsafeUnzip() throws IOException {
        String pocName = "poc.txt";
        String pocZipName = "poc.zip";
        URL url = Thread.currentThread().getContextClassLoader().getResource(pocName);
        File rootPath = new File(url.getPath().toString().replace("/F:/", "F:\\")).getParentFile();
        File pocPath = new File(rootPath, pocName);
        File pocZipPath = new File(rootPath, pocZipName);
        zip(pocPath.getPath(), pocZipPath.getPath());     // zip an poc
        NioUtils.unzip(pocZipPath.getPath(), rootPath.getPath());  // unzip the poc and will throw the IoException
    }

    // zip the poc
    private static void zip(String filePath, String zipPath) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipPath));
            String srcFile = "..\\poc2.txt";
            zos.putNextEntry(new ZipEntry(srcFile));
            FileInputStream in = new FileInputStream(filePath);
            int len;
            byte[] buf = new byte[1024];
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
            in.close();
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
