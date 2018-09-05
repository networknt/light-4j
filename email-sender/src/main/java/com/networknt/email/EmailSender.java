package com.networknt.email;

import com.networknt.common.DecryptUtil;
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
    static final Map<String, Object> secret = DecryptUtil.decryptMap(Config.getInstance().getJsonMapConfig(CONFIG_SECRET));

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

        SMTPAuthenticator auth = new SMTPAuthenticator(emailConfg.getUser(), (String)secret.get(SecretConstants.EMAIL_PASSWORD));
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

        SMTPAuthenticator auth = new SMTPAuthenticator(emailConfg.getUser(), (String)secret.get(SecretConstants.EMAIL_PASSWORD));
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
