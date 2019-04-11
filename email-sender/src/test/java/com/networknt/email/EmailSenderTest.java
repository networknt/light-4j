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

package com.networknt.email;

import org.junit.Ignore;
import org.junit.Test;

import javax.mail.MessagingException;
import java.io.File;

public class EmailSenderTest {
    @Test
    @Ignore
    public void testEmail() {
        EmailSender sender = new EmailSender();
        try {
            sender.sendMail("stevehu@gmail.com", "test", "This is a test email");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testEmailWithAttachment() {
        EmailSender sender = new EmailSender();
        try {
            File file = new File("pom.xml");
            String absolutePath = file.getAbsolutePath();
            sender.sendMailWithAttachment("stevehu@gmail.com", "with attachment", "This is message body", absolutePath);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
