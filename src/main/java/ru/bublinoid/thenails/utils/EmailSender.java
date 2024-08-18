package ru.bublinoid.thenails.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.bublinoid.thenails.config.EmailConfig;

import java.util.Properties;

/**
 * Component responsible for sending emails using SMTP configuration provided by EmailConfig.
 */
@Component
public class EmailSender {

    private final EmailConfig emailConfig;
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    @Autowired
    public EmailSender(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }

    public void sendEmail(String to, String subject, String content) {
        Properties mailProperties = getMailProperties();
        Session session = createSession(mailProperties);
        try {
            MimeMessage message = createMessage(session, to, subject, content);
            Transport.send(message);
            log.debug("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", emailConfig.getSmtp().isAuth());
        props.put("mail.smtp.starttls.enable", emailConfig.getSmtp().getStarttls().isEnable());
        props.put("mail.smtp.starttls.required", emailConfig.getSmtp().getStarttls().isRequired());
        props.put("mail.smtp.host", emailConfig.getSmtp().getHost());
        props.put("mail.smtp.port", emailConfig.getSmtp().getPort());
        props.put("mail.smtp.ssl.trust", emailConfig.getSmtp().getSsl().getTrust());
        props.put("mail.smtp.ssl.protocols", emailConfig.getSmtp().getSsl().getProtocols());
        return props;
    }

    private Session createSession(Properties props) {
        return Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfig.getUsername(), emailConfig.getPassword());
            }
        });
    }

    private MimeMessage createMessage(Session session, String to, String subject, String content) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailConfig.getUsername()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(content, "text/html; charset=UTF-8");
        return message;
    }
}
