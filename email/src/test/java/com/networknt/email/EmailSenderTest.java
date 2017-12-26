package com.networknt.email;

import org.junit.Test;

import javax.mail.MessagingException;

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
}
