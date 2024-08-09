package ru.bublinoid.thenails.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final EmailSender emailSender;
    private final TelegramBot telegramBot;
    private final Map<Long, String> userEmails = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(EmailRepository emailRepository, EmailSender emailSender, @Lazy TelegramBot telegramBot) {
        this.emailRepository = emailRepository;
        this.emailSender = emailSender;
        this.telegramBot = telegramBot;
    }

    public void handleEmailInput(long chatId, String email) {
        if (EmailValidator.isValid(email)) {
            Optional<Email> existingEmail = emailRepository.findByChatId(chatId);

            Email emailEntity;
            if (existingEmail.isPresent()) {
                emailEntity = existingEmail.get();
                if (emailEntity.getConfirm()) {
                    logger.info("Email уже подтвержден для chatId: {}", chatId);
                    telegramBot.sendEmailAlreadyConfirmedMessage(chatId);
                    return;
                }
            } else {
                // Создаем новый объект Email и генерируем хэш
                emailEntity = new Email();
                emailEntity.setChatId(chatId);
                emailEntity.setEmail(email);
                UUID hash = emailEntity.generateHash();
                emailEntity.setHash(hash);
                logger.info("Generated new hash for email input: {}", hash);
            }

            userEmails.put(chatId, email);
            logger.info("Получен действительный email: {} от chatId: {}", email, chatId);

            String confirmationCode = String.format("%04d", CodeGenerator.generateFourDigitCode());
            emailEntity.setConfirmationCode(confirmationCode);

            // Сохраняем в базу данных
            emailRepository.save(emailEntity);
            logger.info("Сохранение email с хэшем: {}", emailEntity.getHash());

            String subject = "Ваш код подтверждения";
            String content = buildConfirmationEmailContent(confirmationCode);

            // Отправляем email
            emailSender.sendEmail(email, subject, content);
            logger.info("Отправлен email на: {}", email);

            telegramBot.sendEmailSavedMessage(chatId);
        } else {
            logger.warn("Получен недействительный email: {} от chatId: {}", email, chatId);
            telegramBot.sendInvalidEmailMessage(chatId);
        }
    }

    public void confirmEmailCode(long chatId, String code) {
        try {
            int inputCode = Integer.parseInt(code);

            Optional<Email> emailEntityOptional = emailRepository.findByChatId(chatId);
            if (emailEntityOptional.isPresent()) {
                Email emailEntity = emailEntityOptional.get();
                int storedCode = Integer.parseInt(emailEntity.getConfirmationCode());
                if (storedCode == inputCode) {
                    emailEntity.setConfirm(true);
                    emailRepository.save(emailEntity);
                    logger.info("Код подтверждения верный для chatId: {}", chatId);
                    telegramBot.sendEmailConfirmedMessage(chatId);
                } else {
                    logger.warn("Неверный код подтверждения для chatId: {}", chatId);
                    telegramBot.sendInvalidConfirmationCodeMessage(chatId);
                }
            } else {
                logger.warn("Не найден email для chatId: {}", chatId);
                telegramBot.sendInvalidEmailMessage(chatId);
            }
        } catch (NumberFormatException e) {
            logger.warn("Неправильный формат кода подтверждения для chatId: {}", chatId);
            telegramBot.sendInvalidConfirmationCodeFormatMessage(chatId);
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