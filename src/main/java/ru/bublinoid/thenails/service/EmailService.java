package ru.bublinoid.thenails.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bublinoid.thenails.model.Email;
import ru.bublinoid.thenails.repository.EmailRepository;
import ru.bublinoid.thenails.telegram.TelegramBot;
import ru.bublinoid.thenails.utils.CodeGenerator;
import ru.bublinoid.thenails.utils.EmailSender;
import ru.bublinoid.thenails.utils.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class responsible for handling email-related operations including
 * email validation, sending confirmation codes, and confirming email addresses.
 */

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final EmailSender emailSender;
    private final TelegramBot telegramBot;
    private final MessageService messageService;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(EmailRepository emailRepository, EmailSender emailSender, @Lazy TelegramBot telegramBot, MessageService messageService) {
        this.emailRepository = emailRepository;
        this.emailSender = emailSender;
        this.telegramBot = telegramBot;
        this.messageService = messageService;
    }

    /**
     * Handles the email input from the user.
     * a confirmation code or informs the user if the email is invalid.
     *
     * @param chatId the chat ID of the user.
     * @param email  the email address provided by the user.
     */
    @Transactional
    public void handleEmailInput(long chatId, String email) {
        // TODO: перенести проверку подтвержденного email на этап выше
        if (EmailValidator.isValid(email)) {
            Email emailEntity = findOrCreateEmailEntity(chatId, email);

            if (emailEntity.getConfirm()) {
                logger.debug("Email already confirmed for chatId: {}", chatId);
                messageService.sendEmailAlreadyConfirmedMessage(chatId);
                return;
            }

            saveEmailAndSendConfirmation(emailEntity, chatId, email);
        } else {
            logger.warn("Received invalid email: {} from chatId: {}", email, chatId);
            messageService.sendInvalidEmailMessage(chatId);

            // Reset confirmation code waiting state as the email is invalid
            telegramBot.setAwaitingConfirmationCodeInput(chatId, false);
        }
    }


    /**
     * Handles the confirmation code input from the user.
     *
     * @param chatId the chat ID of the user.
     * @param code   the confirmation code provided by the user.
     */
    @Transactional
    public void confirmEmailCode(long chatId, String code) {
        // Check if the code is 4 digits long
        if (code.length() != 4 || !code.matches("\\d{4}")) {
            logger.warn("Invalid confirmation code format for chatId: {}", chatId);
            messageService.sendInvalidConfirmationCodeFormatMessage(chatId);
            telegramBot.setAwaitingConfirmationCodeInput(chatId, true);// Expecting re-entry of the code
            return;
        }

        try {
            int inputCode = Integer.parseInt(code);
            Optional<Email> emailEntityOptional = emailRepository.findByChatId(chatId);

            if (emailEntityOptional.isPresent()) {
                validateAndConfirmCode(emailEntityOptional.get(), inputCode, chatId);
            } else {
                logger.warn("Email not found for chatId: {}", chatId);
                messageService.sendInvalidEmailMessage(chatId);
            }
        } catch (NumberFormatException e) {
            logger.debug("Invalid confirmation code format for chatId: {}", chatId);
            messageService.sendInvalidConfirmationCodeFormatMessage(chatId);
            telegramBot.setAwaitingConfirmationCodeInput(chatId, true); // Expecting re-entry of the code
        }
    }

    private Email findOrCreateEmailEntity(long chatId, String email) {
        return emailRepository.findByChatId(chatId)
                .orElseGet(() -> createNewEmailEntity(chatId, email));
    }

    private Email createNewEmailEntity(long chatId, String email) {
        Email emailEntity = new Email();
        emailEntity.setChatId(chatId);
        emailEntity.setEmail(email);
        UUID hash = emailEntity.generateHash();
        emailEntity.setHash(hash);
        emailEntity.setConfirm(false);
        logger.debug("Generated new hash for email input: {}", hash);
        return emailEntity;
    }

    private void saveEmailAndSendConfirmation(Email emailEntity, long chatId, String email) {
        String confirmationCode = String.format("%04d", CodeGenerator.generateFourDigitCode());
        emailEntity.setConfirmationCode(confirmationCode);

        emailRepository.save(emailEntity);
        logger.info("Saved email with hash: {}", emailEntity.getHash());

        String subject = "Ваш код подтверждения";
        String content = buildConfirmationEmailContent(confirmationCode);
        emailSender.sendEmail(email, subject, content);
        logger.debug("Sent confirmation email to: {}", email);

        messageService.sendEmailSavedMessage(chatId);
    }

    private void validateAndConfirmCode(Email emailEntity, int inputCode, long chatId) {
        int storedCode = Integer.parseInt(emailEntity.getConfirmationCode());
        if (storedCode == inputCode) {
            emailEntity.setConfirm(true);
            emailRepository.save(emailEntity);
            logger.debug("Confirmation code is correct for chatId: {}", chatId);
            messageService.sendEmailConfirmedMessage(chatId);
        } else {
            logger.debug("Incorrect confirmation code for chatId: {}", chatId);
            messageService.sendInvalidConfirmationCodeMessage(chatId);
            telegramBot.setAwaitingConfirmationCodeInput(chatId, true);
        }
    }

    /**
     * Builds the content of the confirmation email using a template.
     *
     * @param confirmationCode the confirmation code to include in the email.
     * @return the content of the confirmation email.
     */
    private String buildConfirmationEmailContent(String confirmationCode) {
        try {
            String template = new String(Files.readAllBytes(Paths.get("src/main/resources/email/confirmation_email_template.html")));
            return template.replace("{{confirmationCode}}", confirmationCode);
        } catch (IOException e) {
            logger.error("Error reading email template", e);
            throw new RuntimeException("Error reading email template", e);
        }
    }
}
