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

import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.sun.mail.util.MailSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;

/**
 * Email sender that support both text and attachment.
 *
 * @author Steve Hu
 */
public class EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
    public static final String CONFIG_EMAIL = "email";
    public static final String CONFIG_SECRET = "secret";

    static final EmailConfig emailConfg = (EmailConfig)Config.getInstance().getJsonObjectConfig(CONFIG_EMAIL, EmailConfig.class);

    public EmailSender() {
    }

    /**
     * Send email with a string content.
     *
     * @param to destination email address
     * @param subject email subject
     * @param content email content
     * @throws MessagingException message exception
     */
    public void sendMail (String to, String subject, String content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.user", emailConfg.getUser());
        props.put("mail.smtp.host", emailConfg.getHost());
        props.put("mail.smtp.port", emailConfg.getPort());
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.debug", emailConfg.getDebug());
        props.put("mail.smtp.auth", emailConfg.getAuth());
        props.put("mail.smtp.ssl.trust", emailConfg.host);

        String pass = emailConfg.getPass();
        if(pass == null) {
            Map<String, Object> secret = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
            pass = (String)secret.get(SecretConstants.EMAIL_PASSWORD);
        }

        SMTPAuthenticator auth = new SMTPAuthenticator(emailConfg.getUser(), pass);
        Session session = Session.getInstance(props, auth);

        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(emailConfg.getUser()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

        message.setSubject(subject);

        message.setContent(content, "text/html");

        // Send message
        Transport.send(message);
        if(logger.isInfoEnabled()) logger.info("An email has been sent to " + to + " with subject " + subject);
    }

    /**
     * Send email with a string content and attachment
     *
     * @param to destination eamil address
     * @param subject email subject
     * @param content email content
     * @param filename attachment filename
     * @throws MessagingException messaging exception
     */
    public void sendMailWithAttachment (String to, String subject, String content, String filename) throws MessagingException{
        Properties props = new Properties();
        props.put("mail.smtp.user", emailConfg.getUser());
        props.put("mail.smtp.host", emailConfg.getHost());
        props.put("mail.smtp.port", emailConfg.getPort());
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.debug", emailConfg.getDebug());
        props.put("mail.smtp.auth", emailConfg.getAuth());
        props.put("mail.smtp.ssl.trust", emailConfg.host);

        String pass = emailConfg.getPass();
        if(pass == null) {
            Map<String, Object> secret = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
            pass = (String)secret.get(SecretConstants.EMAIL_PASSWORD);
        }

        SMTPAuthenticator auth = new SMTPAuthenticator(emailConfg.getUser(), pass);
        Session session = Session.getInstance(props, auth);

        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(emailConfg.getUser()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);

        // Create the message part
        BodyPart messageBodyPart = new MimeBodyPart();

        // Now set the actual message
        messageBodyPart.setText(content);

        // Create a multipar message
        Multipart multipart = new MimeMultipart();

        // Set text message part
        multipart.addBodyPart(messageBodyPart);

        // Part two is attachment
        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);

        // Send the complete message parts
        message.setContent(multipart);

        // Send message
        Transport.send(message);
        if(logger.isInfoEnabled()) logger.info("An email has been sent to " + to + " with subject " + subject);
    }

    private static class SMTPAuthenticator extends Authenticator {
        public  String user;
        public  String password;

        public SMTPAuthenticator(String user, String password) {
            this.user = user;
            this.password = password ;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(this.user, this.password);
        }
    }
}
