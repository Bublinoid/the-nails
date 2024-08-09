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

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final EmailSender emailSender;
    private final TelegramBot telegramBot;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(EmailRepository emailRepository, EmailSender emailSender, @Lazy TelegramBot telegramBot) {
        this.emailRepository = emailRepository;
        this.emailSender = emailSender;
        this.telegramBot = telegramBot;
    }

    @Transactional
    public void handleEmailInput(long chatId, String email) {
        if (EmailValidator.isValid(email)) {
            Email emailEntity = findOrCreateEmailEntity(chatId, email);

            if (emailEntity.getConfirm()) {
                logger.info("Email уже подтвержден для chatId: {}", chatId);
                telegramBot.sendEmailAlreadyConfirmedMessage(chatId);
                return;
            }

            saveEmailAndSendConfirmation(emailEntity, chatId, email);
        } else {
            logger.warn("Получен недействительный email: {} от chatId: {}", email, chatId);
            telegramBot.sendInvalidEmailMessage(chatId);
        }
    }

    @Transactional
    public void confirmEmailCode(long chatId, String code) {
        // Проверяем, что код состоит из 4 цифр
        if (code.length() != 4 || !code.matches("\\d{4}")) {
            logger.warn("Неправильный формат кода подтверждения для chatId: {}", chatId);
            telegramBot.sendInvalidConfirmationCodeFormatMessage(chatId);
            telegramBot.setAwaitingConfirmationCodeInput(chatId, true); // Ожидаем повторный ввод кода
            return;
        }

        try {
            int inputCode = Integer.parseInt(code);
            Optional<Email> emailEntityOptional = emailRepository.findByChatId(chatId);

            if (emailEntityOptional.isPresent()) {
                validateAndConfirmCode(emailEntityOptional.get(), inputCode, chatId);
            } else {
                logger.warn("Не найден email для chatId: {}", chatId);
                telegramBot.sendInvalidEmailMessage(chatId);
            }
        } catch (NumberFormatException e) {
            logger.warn("Неправильный формат кода подтверждения для chatId: {}", chatId);
            telegramBot.sendInvalidConfirmationCodeFormatMessage(chatId);
            telegramBot.setAwaitingConfirmationCodeInput(chatId, true); // Ожидаем повторный ввод кода
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
        logger.info("Generated new hash for email input: {}", hash);
        return emailEntity;
    }

    private void saveEmailAndSendConfirmation(Email emailEntity, long chatId, String email) {
        String confirmationCode = String.format("%04d", CodeGenerator.generateFourDigitCode());
        emailEntity.setConfirmationCode(confirmationCode);

        emailRepository.save(emailEntity);
        logger.info("Сохранение email с хэшем: {}", emailEntity.getHash());

        String subject = "Ваш код подтверждения";
        String content = buildConfirmationEmailContent(confirmationCode);
        emailSender.sendEmail(email, subject, content);
        logger.info("Отправлен email на: {}", email);

        telegramBot.sendEmailSavedMessage(chatId);
    }

    private void validateAndConfirmCode(Email emailEntity, int inputCode, long chatId) {
        int storedCode = Integer.parseInt(emailEntity.getConfirmationCode());
        if (storedCode == inputCode) {
            emailEntity.setConfirm(true);
            emailRepository.save(emailEntity);
            logger.info("Код подтверждения верный для chatId: {}", chatId);
            telegramBot.sendEmailConfirmedMessage(chatId);
        } else {
            logger.warn("Неверный код подтверждения для chatId: {}", chatId);
            telegramBot.sendInvalidConfirmationCodeMessage(chatId);
            telegramBot.setAwaitingConfirmationCodeInput(chatId, true);
        }
    }

    private String buildConfirmationEmailContent(String confirmationCode) {
        try {
            String template = new String(Files.readAllBytes(Paths.get("src/main/resources/email/confirmation_email_template.html")));
            return template.replace("{{confirmationCode}}", confirmationCode);
        } catch (IOException e) {
            logger.error("Ошибка чтения шаблона email", e);
            throw new RuntimeException("Ошибка чтения шаблона email", e);
        }
    }
}
