package com.networknt.email;

import com.networknt.common.SecretConfig;
import com.networknt.config.Config;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {
    public static final String CONFIG_EMAIL = "email";
    public static final String CONFIG_SECRET = "secret";

    static final EmailConfig emailConfg = (EmailConfig)Config.getInstance().getJsonObjectConfig(CONFIG_EMAIL, EmailConfig.class);
    static final SecretConfig secretConfig = (SecretConfig)Config.getInstance().getJsonObjectConfig(CONFIG_SECRET, SecretConfig.class);

    public EmailSender() {
    }

    public void sendMail (String to, String subject, String content) throws MessagingException{
        Properties props = new Properties();
        props.put("mail.smtp.user", emailConfg.getUser());
        props.put("mail.smtp.host", emailConfg.getHost());
        props.put("mail.smtp.port", emailConfg.getPort());
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.debug", emailConfg.getDebug());
        props.put("mail.smtp.auth", emailConfg.getAuth());

        SMTPAuthenticator auth = new SMTPAuthenticator(emailConfg.getUser(), secretConfig.getEmailPassword());
        Session session = Session.getInstance(props, auth);

        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(emailConfg.getUser()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

        message.setSubject(subject);

        message.setContent(content, "text/html");

        // Send message
        Transport.send(message);
    }

    private class SMTPAuthenticator extends Authenticator {
        public  String user;
        public  String password;

        public SMTPAuthenticator(String user, String password) {
            this.user = user;
            this.password = password ;
        }

        public PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(this.user, this.password);
        }
    }
}
