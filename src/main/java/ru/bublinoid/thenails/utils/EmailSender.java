package ru.bublinoid.thenails.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.bublinoid.thenails.config.EmailConfig;

import java.util.Properties;

@Component
public class EmailSender {

    private final EmailConfig emailConfig;
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    @Autowired
    public EmailSender(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }

    public void sendEmail(String to, String subject, String content) {
        final String username = emailConfig.getUsername();
        final String password = emailConfig.getPassword();
        final boolean smtpAuth = emailConfig.getSmtp().isAuth();
        final boolean starttlsEnable = emailConfig.getSmtp().getStarttls().isEnable();
        final boolean starttlsRequired = emailConfig.getSmtp().getStarttls().isRequired();
        final String smtpHost = emailConfig.getSmtp().getHost();
        final int smtpPort = emailConfig.getSmtp().getPort();
        final String sslTrust = emailConfig.getSmtp().getSsl().getTrust();
        final String sslProtocols = emailConfig.getSmtp().getSsl().getProtocols();

        Properties props = new Properties();
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", sslTrust);
        props.put("mail.smtp.ssl.protocols", sslProtocols);
        //props.put("mail.debug", "true");

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);

            log.info("Email sent successfully");

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
