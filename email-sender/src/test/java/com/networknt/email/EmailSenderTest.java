package com.networknt.email;

import org.junit.Test;

import javax.mail.MessagingException;
import java.io.File;

public class EmailSenderTest {
    //@Test
    public void testEmail() {
        EmailSender sender = new EmailSender();
        try {
            sender.sendMail("stevehu@gmail.com", "test", "This is a test email");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    //@Test
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
