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

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email sender that support both text and attachment.
 *
 * @author Steve Hu
 */
public class EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
    private String configName;

    public EmailSender() {
        this(EmailConfig.CONFIG_NAME);
    }

    public EmailSender(String configName) {
        this.configName = configName;
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
        EmailConfig config = EmailConfig.load(configName);
        Properties props = new Properties();
        props.put("mail.smtp.user", config.getUser());
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", config.getPort());
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.debug", config.getDebug());
        props.put("mail.smtp.auth", config.getAuth());
        props.put("mail.smtp.ssl.trust", config.getHost());

        String pass = config.getPass();

        SMTPAuthenticator auth = new SMTPAuthenticator(config.getUser(), pass);
        Session session = Session.getInstance(props, auth);

        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(config.getUser()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

        message.setSubject(subject);

        message.setContent(content, "text/html");

        // Send message
        Transport.send(message);
        if(logger.isInfoEnabled()) logger.info("An email has been sent to {} with subject {}", to, subject);
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
        EmailConfig config = EmailConfig.load(configName);
        Properties props = new Properties();
        props.put("mail.smtp.user", config.getUser());
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", config.getPort());
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.debug", config.getDebug());
        props.put("mail.smtp.auth", config.getAuth());
        props.put("mail.smtp.ssl.trust", config.getHost());

        String pass = config.getPass();

        SMTPAuthenticator auth = new SMTPAuthenticator(config.getUser(), pass);
        Session session = Session.getInstance(props, auth);

        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(config.getUser()));
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
        if(logger.isInfoEnabled()) logger.info("An email has been sent to {} with subject {}", to, subject);
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

    /**
     * This is the template variable replacement utility to replace [name] with a key
     * name in the map with the value in the template to generate the final email body.
     *
     * @param text The template in html format
     * @param replacements A map that contains key/value pair for variables
     * @return String of processed template
     */
    public static String replaceTokens(String text,
                                       Map<String, String> replacements) {
        Pattern pattern = Pattern.compile("\\[(.+?)]");
        Matcher matcher = pattern.matcher(text);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String replacement = replacements.get(matcher.group(1));
            if (replacement != null) {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
